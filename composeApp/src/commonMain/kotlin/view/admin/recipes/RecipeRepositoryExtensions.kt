package view.admin.recipes

import data.EventRepository
import model.Recipe

/**
 * Extension functions for recipe CRUD operations
 * These methods are now part of the EventRepository interface
 * and will be implemented in FireBaseRepository
 */

// These extension functions are no longer needed since the methods
// are now directly in the EventRepository interface.
// The ViewModel will call the repository methods directly:
// - eventRepository.createRecipe(recipe)
// - eventRepository.updateRecipe(recipe)  
// - eventRepository.deleteRecipe(recipeId)