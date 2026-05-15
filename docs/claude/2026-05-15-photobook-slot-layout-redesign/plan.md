# Implementation Plan

## Phase 0: Safety And Baseline

- [ ] Run `git status --short`.
- [ ] Note unrelated untracked files and do not stage them.
- [ ] Confirm current branch and latest commit.
- [ ] Inspect current photobook editor, preview, PDF export, model, repository, and database code.
- [ ] Do not overwrite recent logo or cover/back-cover changes.

Recommended commands:

```bash
git status --short
git branch --show-current
git rev-parse --short HEAD
```

## Phase 1: New Model

- [ ] Add `PageTemplate`.
- [ ] Add `ImageRef`.
- [ ] Add `ImageSlot`.
- [ ] Add `FitMode`.
- [ ] Replace or adapt `PageState` to use `template` and `slots`.
- [ ] Add `PageLayoutDocument`.
- [ ] Update serialization/deserialization to use the new page layout document.
- [ ] Do not implement legacy conversion.

Verification:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```

## Phase 2: Shared Layout Engine

- [ ] Add `TemplateLayoutEngine`.
- [ ] Implement slot rectangles for:
  - `Single`
  - `TwoHorizontal`
  - `TwoVertical`
  - `GridFour`
- [ ] Respect trim size, bleed, safe margins, and gutters.
- [ ] Add focused unit tests for calculated slot count and bounds.

Verification:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest
```

## Phase 3: Content Page Rendering

- [ ] Update editor content-page rendering to draw slots from `TemplateLayoutEngine`.
- [ ] Update slot image rendering to use `ImageSlot.imageRef`.
- [ ] Implement crop clipping and crop transform.
- [ ] Replace element-index selection with slot selection.
- [ ] Preserve current cover/back-cover editor pages.

Risk points:

- Image gestures must update selected slot crop values.
- Crop state must be saved only when Save is tapped if that is the current editor contract.
- Empty slots must be visually clickable without breaking layout.

## Phase 4: Template Editing

- [ ] Add template switch UI for content pages.
- [ ] Implement template change algorithm.
- [ ] Preserve images by slot order when expanding template capacity.
- [ ] Show overflow confirmation when shrinking template capacity.
- [ ] Do not auto-absorb photos from adjacent pages.

Manual checks:

- Single to GridFour keeps original image in first slot.
- GridFour to Single with multiple images asks for confirmation.
- Cancel leaves the page unchanged.

## Phase 5: Memory Photo Picker

- [ ] Update picker contract to return selected photos.
- [ ] Support batch import mode.
- [ ] Support single slot mode.
- [ ] Whole-memory selection imports all photos in that memory.
- [ ] Individual photo selection imports only selected photos.
- [ ] Batch import creates one `Single` content page per selected photo.
- [ ] Single slot mode fills or replaces one slot only.

Risk points:

- A memory with multiple images must not collapse to only the first image.
- URI permissions must still work after reopening the photobook.

## Phase 6: Move And Swap Commands

- [ ] Add commands for move to previous page, next page, new page, and chosen page.
- [ ] Implement first-empty-slot target behavior.
- [ ] Implement target-full dialog.
- [ ] Keep source page if it becomes empty.
- [ ] Implement same-page slot swap.

Manual checks:

- Move from page 2 to page 1 when page 1 has empty slot.
- Move from page 2 to page 1 when page 1 is full.
- Move image away from a page and confirm empty page remains.
- Swap two images inside GridFour.

## Phase 7: Preview Rewrite Around Leaves

- [ ] Build preview leaf list: cover + content + back cover.
- [ ] Keep portrait one-page behavior.
- [ ] Keep landscape two-page spread behavior.
- [ ] Make cover the first preview page.
- [ ] Make back cover the final preview page.
- [ ] Use the same content-page renderer/layout engine as editor where possible.

Manual checks:

- Preview opens on cover.
- Next navigation does not jump back to cover after page 2.
- Landscape spread pages are joined.
- Final navigation reaches back cover.

## Phase 8: PDF Export

- [ ] Export cover first.
- [ ] Export all content pages next.
- [ ] Export back cover last.
- [ ] Use `TemplateLayoutEngine` for content pages.
- [ ] Use saved slot crop values.
- [ ] Validate exported PDF page count is content pages plus 2.

Manual checks:

- PDF first page is cover.
- PDF last page is back cover.
- Multi-slot pages match editor/preview layout.
- Images do not overlap mood/date or printable-safe areas.

## Phase 9: Regression And APK

- [ ] Run unit tests.
- [ ] Build debug APK.
- [ ] Install or manually test on device.
- [ ] Review git diff.
- [ ] Stage only intended source/docs/schema files.
- [ ] Do not stage `.DS_Store`, `.gradle`, `build`, `.idea`, APK outputs, or unrelated directories.

Commands:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest :app:assembleDebug
```

Expected APK:

```text
/Users/haos/Project/Android/YingJian/app/build/outputs/apk/debug/app-debug.apk
```

