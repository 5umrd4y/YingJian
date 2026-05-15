# Photobook Slot Layout Redesign Handoff

Date: 2026-05-15
Owner: Claude implementation, Codex review/validation
Project: `/Users/haos/Project/Android/YingJian`

## Goal

Redesign the photobook editor, preview, and PDF export around a stable page-template and image-slot model.

The target product is not a free-form canvas editor. It is a template-based photobook tool with predictable printing output:

- Content pages support fixed layouts: single image, two horizontal images, two vertical images, and four-grid.
- Imported photos default to one photo per page, proportional scaling, centered inside the page layout.
- Users can change a page template manually.
- Users can fill slots from memory photos, move photos between pages, swap photos within a page, and adjust crop position/zoom inside a slot.
- Cover and back cover are first-class pages for editor, preview, and PDF export, but do not contain image slots in this phase.
- Preview keeps current orientation behavior: portrait shows one page, landscape shows two joined pages.
- PDF export uses the same layout rules as editor and preview.

## Non-Goals

- Do not build a full free-form design editor.
- Do not support arbitrary image rotation, arbitrary text boxes, stickers, or decorative layers in this phase.
- Do not auto-merge pages when a user changes a page to a multi-image layout.
- Do not support cover/back-cover images in this phase.
- Do not preserve old photobook layout data. The app is still in testing and can be reinstalled.

## Recommended Workflow

1. Read `requirements.md`, then `spec.md`.
2. Implement phase-by-phase from `plan.md`.
3. Run the validation commands in `validation.md` after each major phase.
4. Do not stage or revert unrelated worktree changes.
5. When Claude finishes, ask Codex to review using `validation.md`.

## Current Design Decision Summary

- Use `PageTemplate + ImageSlot + ImageRef` as the core model.
- Batch import from memories produces pages at the photo level, not the memory level.
- A selected image can be moved by commands, not by dragging across pages.
- If a move leaves the source page empty, keep that empty page. The user deletes it manually.
- If a target page has empty slots, put the moved image into the first empty slot.
- If a target page is full, ask the user to replace, change layout, move to a new page, or cancel.
- Changing a page layout does not absorb photos from adjacent pages.
- Editor, preview, and PDF must share one layout engine.

