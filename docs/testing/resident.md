# Resident screens: accessibility and real-phone check

## Lighthouse accessibility (Chrome DevTools, mobile)

| Screen                    | Before | After |
| ------------------------- | ------ | ----- |
| R1 landing                | 95     | 100   |
| R2 choose document        | 100    | 100   |
| R5 ticket (Waiting state) | 100    | 100   |

All three meet the 95+ target. R5 was tested on a real ticket in the Waiting state only.

## Real-phone check

- **Device:** iPhone 12 Pro, iOS <version>, <Safari/Chrome>
- **Text size:** largest
- **Site tested:** pilago-sanisidro.onrender.com
- **Screens checked:** landing, choose document, details, review, ticket, find ticket
- **Result:** No cut-off or overlapping text found at the largest text size.

## Bugs

| #   | Screen     | Problem                                                                           | Screenshot          | Status                                           |
| --- | ---------- | --------------------------------------------------------------------------------- | ------------------- | ------------------------------------------------ |
| 1   | R1 landing | "Staff login" and "Display board" links had contrast of about 4.3:1 (needs 4.5:1) | Fixed: `.pg-link` now uses `--text-brand-strong` |

No bugs found in the real-phone check.
