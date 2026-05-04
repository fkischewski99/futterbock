package data.local

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    // Instant <-> Long
    @TypeConverter fun fromInstant(value: Instant): Long = value.toEpochMilliseconds()
    @TypeConverter fun toInstant(value: Long): Instant = Instant.fromEpochMilliseconds(value)

    @TypeConverter fun fromInstantNullable(value: Instant?): Long? = value?.toEpochMilliseconds()
    @TypeConverter fun toInstantNullable(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    // LocalDate <-> String
    @TypeConverter fun fromLocalDate(value: LocalDate): String = value.toString()
    @TypeConverter fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    // List<String>
    @TypeConverter fun fromStringList(value: List<String>): String = json.encodeToString(value)
    @TypeConverter fun toStringList(value: String): List<String> = json.decodeFromString(value)

    // MutableSet<String>
    @TypeConverter fun fromStringSet(value: MutableSet<String>): String = json.encodeToString(value.toList())
    @TypeConverter fun toStringSet(value: String): MutableSet<String> = json.decodeFromString<List<String>>(value).toMutableSet()

    // List<FoodIntolerance>
    @TypeConverter fun fromFoodIntoleranceList(value: List<FoodIntolerance>): String = json.encodeToString(value.map { it.name })
    @TypeConverter fun toFoodIntoleranceList(value: String): List<FoodIntolerance> = json.decodeFromString<List<String>>(value).map { FoodIntolerance.valueOf(it) }

    // List<Season>
    @TypeConverter fun fromSeasonList(value: List<Season>): String = json.encodeToString(value.map { it.name })
    @TypeConverter fun toSeasonList(value: String): List<Season> = json.decodeFromString<List<String>>(value).map { Season.valueOf(it) }

    // List<RecipeType>
    @TypeConverter fun fromRecipeTypeList(value: List<RecipeType>): String = json.encodeToString(value.map { it.name })
    @TypeConverter fun toRecipeTypeList(value: String): List<RecipeType> = json.decodeFromString<List<String>>(value).map { RecipeType.valueOf(it) }

    // Enums
    @TypeConverter fun fromEatingHabit(value: EatingHabit): String = value.name
    @TypeConverter fun toEatingHabit(value: String): EatingHabit = EatingHabit.valueOf(value)

    @TypeConverter fun fromEventType(value: EventType): String = value.name
    @TypeConverter fun toEventType(value: String): EventType = EventType.valueOf(value)

    @TypeConverter fun fromMealType(value: MealType): String = value.name
    @TypeConverter fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter fun fromIngredientUnit(value: IngredientUnit): String = value.name
    @TypeConverter fun toIngredientUnit(value: String): IngredientUnit = IngredientUnit.valueOf(value)

    @TypeConverter fun fromIngredientUnitNullable(value: IngredientUnit?): String? = value?.name
    @TypeConverter fun toIngredientUnitNullable(value: String?): IngredientUnit? = value?.let { IngredientUnit.valueOf(it) }

    @TypeConverter fun fromSource(value: Source): String = value.name
    @TypeConverter fun toSource(value: String): Source = Source.valueOf(value)

    @TypeConverter fun fromRange(value: Range): String = value.name
    @TypeConverter fun toRange(value: String): Range = Range.valueOf(value)

    @TypeConverter fun fromTimeRange(value: TimeRange): String = value.name
    @TypeConverter fun toTimeRange(value: String): TimeRange = TimeRange.valueOf(value)

    // Complex nested objects as JSON using kotlinx.serialization
    @TypeConverter fun fromShoppingIngredientList(value: List<ShoppingIngredient>): String = json.encodeToString(value)
    @TypeConverter fun toShoppingIngredientList(value: String): List<ShoppingIngredient> = json.decodeFromString(value)

    @TypeConverter fun fromRecipeSelectionList(value: List<RecipeSelection>): String = json.encodeToString(value)
    @TypeConverter fun toRecipeSelectionList(value: String): List<RecipeSelection> = json.decodeFromString(value)

    @TypeConverter fun fromDailyListsMap(value: Map<LocalDate, DailyShoppingList>): String = json.encodeToString(value)
    @TypeConverter fun toDailyListsMap(value: String): Map<LocalDate, DailyShoppingList> = json.decodeFromString(value)
}
