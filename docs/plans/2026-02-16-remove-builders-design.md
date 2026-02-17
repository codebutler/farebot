# Remove Builder Classes

## Problem

The codebase has 4 Builder classes that are not idiomatic Kotlin. Kotlin data classes with named parameters and default values make Builders unnecessary.

## Builders to Remove

### 1. ClipperTrip.Builder
- 10 Long fields, all default to 0
- Replace with: data class constructor with named params, all defaulting to 0

### 2. SeqGoTrip.Builder
- 8 fields (ints, nullable Instants, nullable Stations, Mode enum)
- Replace with: data class constructor with named params and defaults

### 3. Station.Builder
- 8 fields (nullable Strings, one List<String>)
- Replace with: data class constructor with named params, all defaulting to null/emptyList()

### 4. FareBotUiTree.Builder + Item.Builder
- Hierarchical builder for UI tree structures
- Item.title is currently `String`, resolved from `FormattedString` during `suspend build()`
- `@Serializable` annotation is unnecessary (never serialized)
- Replace with:
  - Drop `@Serializable` from FareBotUiTree and Item
  - Change `Item.title` from `String` to `FormattedString`
  - Remove both Builder classes
  - Rewrite `uiTree {}` DSL to build Item objects directly
  - Update all ~20 call sites that use the builder directly

## Approach

- Change `Item.title` to `FormattedString`, resolve at the UI layer instead
- Rewrite `UiTreeBuilder.kt` DSL to construct items directly (no intermediate Builder)
- For ClipperTrip, SeqGoTrip, Station: add default parameter values, remove Builder + companion factory
- Update all call sites
