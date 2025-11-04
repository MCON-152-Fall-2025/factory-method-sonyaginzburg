package com.mcon152.recipeshare.service;

import com.mcon152.recipeshare.BasicRecipe;
import com.mcon152.recipeshare.DessertRecipe;
import com.mcon152.recipeshare.Recipe;
import com.mcon152.recipeshare.VegetarianRecipe;
import com.mcon152.recipeshare.repository.RecipeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Assignment: Implement all TODOs using Mockito features covered in class:
 *  - @Mock, @InjectMocks, @Captor, @ExtendWith(MockitoExtension.class)
 *  - Stubbing: thenReturn / thenAnswer / thenThrow
 *  - Verifications: verify(...), times/never/atLeast..., verifyNoMoreInteractions
 *  - InOrder (where meaningful)
 *  - Void stubbing: doNothing / doThrow (use deleteById for this)
 *  - Matchers: any(), eq(), argThat()
 *  - ArgumentCaptor
 *  - (Optional) Spy demo if you introduce a small helper in tests
 *
 * NOTE: This is a pure unit test. Do NOT start a Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService (Mockito) — Assignment Skeleton")
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService; // CUT implements RecipeService

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    // --- Helpers for sample data ---

    private Recipe newRecipeNoId() {
        return new Recipe(
                null,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    private Recipe savedRecipe(long id) {
        return new Recipe(
                id,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    // ------------------ addRecipe ------------------

    @Nested
    @DisplayName("addRecipe(Recipe)")
    class AddRecipe {

        @Test
        @DisplayName("returns saved entity (thenReturn) and calls repository.save once")
        void returnsSaved_andSavesOnce() {
            Recipe input = newRecipeNoId();
            Recipe saved = savedRecipe(1L);

            when(recipeRepository.save(any(Recipe.class))).thenReturn(saved);

            Recipe out = recipeService.addRecipe(input);
            assertEquals(1L, out.getId());
            assertEquals(saved, out);

            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("assigns ID dynamically (thenAnswer) and captures argument")
        void assignsId_thenAnswer_andCaptures() {
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
                Recipe r = inv.getArgument(0);
                return new Recipe(1L, r.getTitle(), r.getDescription(),
                        r.getIngredients(), r.getInstructions(), r.getServings());
            });

            Recipe out = recipeService.addRecipe(newRecipeNoId());
            assertEquals(1L, out.getId());

            verify(recipeRepository).save(recipeCaptor.capture());
            Recipe sent = recipeCaptor.getValue();
            assertNull(sent.getId()); // before persistence
            assertEquals("Chocolate Cake", sent.getTitle());
        }

        @Test
        @DisplayName("propagates repository failure (thenThrow)")
        void propagatesRepositoryFailure() {
            when(recipeRepository.save(any())).thenThrow(new IllegalStateException("DB down"));
            assertThrows(IllegalStateException.class, () -> recipeService.addRecipe(newRecipeNoId()));
            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ getAllRecipes ------------------

    @Nested
    @DisplayName("getAllRecipes()")
    class GetAllRecipes {


        @Test
        @DisplayName("returns list from repository")
        void returnsList() {
            List<Recipe> list = List.of(savedRecipe(1L), savedRecipe(2L));
            when(recipeRepository.findAll()).thenReturn(list);

            List<Recipe> out = recipeService.getAllRecipes();
            assertEquals(2, out.size());
            assertEquals(list, out);

            verify(recipeRepository).findAll();
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ getRecipeById ------------------

    @Nested
    @DisplayName("getRecipeById(long)")
    class GetById {

        @Test
        @DisplayName("returns Optional.present when found")
        void present() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(savedRecipe(1L)));
            Optional<Recipe> out = recipeService.getRecipeById(1L);
            assertTrue(out.isPresent());
            assertEquals(1L, out.get().getId());
            verify(recipeRepository).findById(1L);
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("returns Optional.empty when missing")
        void empty() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());
            Optional<Recipe> out = recipeService.getRecipeById(1L);
            assertTrue(out.isEmpty());
            verify(recipeRepository).findById(1L);
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ deleteRecipe ------------------

    @Nested
    @DisplayName("deleteRecipe(long)")
    class DeleteRecipe {

        @Test
        @DisplayName("returns true when entity existed")
        void returnsTrue_whenExists() {
            long id = 5L;
            when(recipeRepository.existsById(id)).thenReturn(true);
            doNothing().when(recipeRepository).deleteById(id);

            boolean out = recipeService.deleteRecipe(id);
            assertTrue(out);

            InOrder in = inOrder(recipeRepository);
            in.verify(recipeRepository).existsById(id);
            in.verify(recipeRepository).deleteById(id);
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("returns false when missing (never deletes)")
        void returnsFalse_whenMissing() {
            long id = 6L;
            when(recipeRepository.existsById(id)).thenReturn(false);

            boolean out = recipeService.deleteRecipe(id);
            assertFalse(out);

            verify(recipeRepository).existsById(id);
            verify(recipeRepository, never()).deleteById(anyLong());
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("propagates delete error (doThrow)")
        void propagatesDeleteError() {
            long id = 7L;
            when(recipeRepository.existsById(id)).thenReturn(true);
            doThrow(new IllegalStateException("constraint")).when(recipeRepository).deleteById(id);

            assertThrows(IllegalStateException.class, () -> recipeService.deleteRecipe(id));

            verify(recipeRepository).existsById(id);
            verify(recipeRepository).deleteById(id);
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ updateRecipe ------------------

    @Nested
    @DisplayName("updateRecipe(long, Recipe)")
    class UpdateRecipe {

        @Test
        @DisplayName("returns updated entity when exists")
        void returnsUpdated_whenExists() {
            long id = 10L;
            Recipe existing = new Recipe(id, "Old", "old", "i", "n", 1);
            Recipe updatedInput = new Recipe(null, "New Title", "New Desc", "new i", "new n", 2);

            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

            Optional<Recipe> out = recipeService.updateRecipe(id, updatedInput);
            assertTrue(out.isPresent());
            Recipe saved = out.get();
            assertEquals("New Title", saved.getTitle());
            assertEquals("New Desc", saved.getDescription());
            assertEquals(Integer.valueOf(2), saved.getServings());

            verify(recipeRepository).findById(id);
            verify(recipeRepository).save(recipeCaptor.capture());
            Recipe sent = recipeCaptor.getValue();
            assertEquals("New Title", sent.getTitle());
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            long id = 11L;
            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            Optional<Recipe> out = recipeService.updateRecipe(id, new Recipe());
            assertTrue(out.isEmpty());
            verify(recipeRepository).findById(id);
            verify(recipeRepository, never()).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("preserves entity subtype on update")
        void preservesSubtype_onUpdate() {
            long id = 33L;
            DessertRecipe existing = new DessertRecipe(id, "OldDessert", "desc", "i", "n", 4);
            Recipe updatedInput = new BasicRecipe(null, "New", "desc", "i2", "n2", 2);

            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

            Optional<Recipe> out = recipeService.updateRecipe(id, updatedInput);
            assertTrue(out.isPresent());
            Recipe saved = out.get();
            assertInstanceOf(DessertRecipe.class, saved);
            assertEquals("New", saved.getTitle());

            verify(recipeRepository).findById(id);
            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ patchRecipe ------------------

    @Nested
    @DisplayName("patchRecipe(long, Recipe)")
    class PatchRecipe {

        @Test
        @DisplayName("applies only non-null fields (argThat)")
        void appliesNonNullFields_only() {
            long id = 20L;
            Recipe existing = new Recipe(id, "Title", "Desc", "I", "N", 3);
            Recipe partial = new Recipe(null, "New Title", null, null, null, null);

            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

            Optional<Recipe> out = recipeService.patchRecipe(id, partial);
            assertTrue(out.isPresent());
            Recipe saved = out.get();
            assertEquals("New Title", saved.getTitle());
            assertEquals("Desc", saved.getDescription());

            // verify save received an object with changed title but unchanged description
            verify(recipeRepository).save(argThat(r -> "New Title".equals(r.getTitle()) && "Desc".equals(r.getDescription())));
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            long id = 21L;
            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            Optional<Recipe> out = recipeService.patchRecipe(id, new Recipe());
            assertTrue(out.isEmpty());
            verify(recipeRepository).findById(id);
            verify(recipeRepository, never()).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);

        }

        @Test
        @DisplayName("preserves entity subtype on patch")
        void preservesSubtype_onPatch() {
            long id = 44L;
            VegetarianRecipe existing = new VegetarianRecipe(id, "OldVeg", "d", "i", "n", 2);
            Recipe partial = new BasicRecipe(null, null, "PatchedDesc", null, null, null);

            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

            Optional<Recipe> out = recipeService.patchRecipe(id, partial);
            assertTrue(out.isPresent());
            Recipe saved = out.get();
            assertInstanceOf(VegetarianRecipe.class, saved);
            assertEquals("PatchedDesc", saved.getDescription());

            verify(recipeRepository).findById(id);
            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ extra practice ------------------

    @Nested
    @DisplayName("Advanced stubbing & verification")
    class Advanced {

        @Test
        @DisplayName("consecutive stubs on existsById (true, false)")
        void consecutiveStubs_existsById() {
            when(recipeRepository.existsById(1L)).thenReturn(true, false);

            boolean first = recipeService.deleteRecipe(1L);
            boolean second = recipeService.deleteRecipe(1L);

            assertTrue(first);
            assertFalse(second);

            verify(recipeRepository, times(2)).existsById(1L);
            // deleteById should have been called only once (first call)
            verify(recipeRepository, times(1)).deleteById(1L);
            verifyNoMoreInteractions(recipeRepository);
        }
    }
}
