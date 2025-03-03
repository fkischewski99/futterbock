package model

enum class RecipeType(val displayName: String) {
    BREAKFAST("Frühstück"),
    BREAD_TIME("Brotzeit"),
    SALAD("Salat"),
    NOODLE("Nudelgerichte"),
    RICE("Reisgerichte"),
    POTATO("Kartoffelgerichte"),
    SOUPS("Suppen und Eintöpfe"),
    DRINKS("Getränke"),
    SNACK("Nachtisch und Snack"),
    BAKING("Backanhänger"),
    IDEAS_FOR_GROUP_SESSION("Ideen für die Gruppenstunde");
    //LUNCH("Mittagessen"),
    //DINNER("Abendessen");

    override fun toString(): String {
        return this.displayName
    }
}