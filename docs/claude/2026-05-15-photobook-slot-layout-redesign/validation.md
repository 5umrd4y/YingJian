# Validation And Review Checklist

Codex should use this checklist to review Claude's implementation.

## Git Hygiene

- [ ] Implementation is on the expected branch.
- [ ] `git status --short` has been reviewed.
- [ ] No unrelated `.DS_Store`, `.gradle`, `build`, `.idea`, APK outputs, or external project files are staged.
- [ ] Existing logo changes are not reverted.
- [ ] Existing cover/back-cover support is not reverted.

## Model Checks

- [ ] Content pages use `PageTemplate`.
- [ ] Content pages use `ImageSlot`.
- [ ] Slot image source uses `ImageRef` or equivalent photo-level reference.
- [ ] Slot crop data includes scale and x/y offset.
- [ ] Persistence saves the new `PageLayoutDocument` or equivalent versioned document.
- [ ] No legacy conversion is required or implemented unless necessary for compile compatibility.

## Layout Engine Checks

- [ ] One shared layout engine calculates slot rectangles.
- [ ] `Single` returns one slot.
- [ ] `TwoHorizontal` returns two top/bottom slots.
- [ ] `TwoVertical` returns two left/right slots.
- [ ] `GridFour` returns four slots.
- [ ] Slot rectangles fit inside the safe content area.
- [ ] Editor, preview, and PDF do not each duplicate independent template math.

## Editor Manual QA

- [ ] Editor opens with cover as the first page.
- [ ] Back cover appears as the last page.
- [ ] Cover/back-cover text can be edited and saved.
- [ ] Batch import of one memory with N photos creates N single-image pages.
- [ ] Importing selected photos creates pages only for selected photos.
- [ ] Imported images are proportional and centered by default.
- [ ] Changing `Single` to `GridFour` keeps the original image in slot 1 and leaves other slots empty.
- [ ] Empty slots can be clicked to select a photo.
- [ ] Slot image can be dragged inside the slot.
- [ ] Slot image can be zoomed inside the slot.
- [ ] Tapping Save persists crop position and zoom.
- [ ] Reopening editor restores crop position and zoom.
- [ ] Moving an image away leaves an empty page if no other images remain.
- [ ] Moving to a page with empty slots fills the first empty slot.
- [ ] Moving to a full page shows a decision dialog.
- [ ] Same-page swap exchanges two slot images correctly.

## Preview QA

- [ ] Preview first page is cover.
- [ ] Portrait orientation shows one page.
- [ ] Landscape orientation shows two joined pages.
- [ ] Navigation reaches every content page.
- [ ] Navigation does not jump from page 2 back to cover.
- [ ] Final page is back cover.
- [ ] Multi-slot page layout matches editor.
- [ ] Slot crop/zoom matches editor.

## PDF QA

- [ ] Exported page count equals content page count plus 2.
- [ ] First PDF page is cover.
- [ ] Last PDF page is back cover.
- [ ] Multi-slot content pages match editor/preview geometry.
- [ ] Crop/zoom is respected.
- [ ] Images do not push mood text/date outside printable layout.
- [ ] Bleed and safe margins remain respected.

## Build Commands

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
```

Expected APK:

```text
/Users/haos/Project/Android/YingJian/app/build/outputs/apk/debug/app-debug.apk
```

## Review Focus

- Off-by-one bugs caused by cover/content/back-cover leaf indexing.
- Accidental single-image assumptions in multi-slot pages.
- Duplicate layout math across editor, preview, and PDF.
- Crop/zoom state not persisted after Save.
- Memory picker still returning only the first photo from a multi-photo memory.
- Page move behavior accidentally deleting empty pages.
- Preview landscape spread pairing around cover and back cover.

