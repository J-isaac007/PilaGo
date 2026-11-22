# PilaGo P5 test report

- Tester: Precious
- Scope: Resident → secretary → captain → secretary release
- Overall result: **Blocked** (the three-device Render run could not start). Checked on 23 November 2026, Philippine time.

## 1. Render link check

| Address tried | Home page | Login page |
|---|---|---|
| https://pilago-sanisidro.onrender.com | 404 | 404 |
| https://pilago-san-isidro.onrender.com | 404 | 404 |

Both addresses were guesses. The address in the repo comments and on the TV board is `pilago-sanisidro.onrender.com`. A 404 usually means the service name is different or the deploy has not been created yet. Ask Isaac for the real address from the Render dashboard.

## 2. Evidence so far

These results were reported by Precious from her own machine. Nobody else has re-run them yet.

| Check | Result | Where it was run |
|---|---|---|
| Resident → secretary → captain → release, in separate browser sessions | Pass. The resident ticket showed Released. | Local (localhost:8080) |
| P3 login lock after 5 wrong passwords | Pass. Warning seen on the laptop. Automated test covers the 5th failure and unlock after 10 minutes. | P3 branch, not merged into main yet |
| P4 CSV report | Pass for the download and the 10 columns. A name with a comma stayed in one column. Excel or Google Sheets check is **pending**. | P4 branch, not merged into main yet |
| Three-device run on Render | **Blocked.** No Render test ticket was created. | Render |

To re-run the automated tests on any branch, open a terminal in the PilaGo folder and run:

```
.\mvnw.cmd verify
```

Write the number of tests that passed here once the P3 and P4 branches are merged: ____ tests, ____ failures.

## 3. Render retest (fill in when the link works)

- Confirmed Render URL:
- Deployment or commit, if known:
- Date and time (Philippines):
- Phone 1 model, system, browser:
- Phone 2 model, system, browser:
- Laptop model, system, browser:
- Test ticket number:

Use the team's approved test accounts and a made-up resident name. Follow the same ticket number on all three devices.

| # | Device | Action | Expected result | Actual result | Pass / Fail / Blocked |
|---|---|---|---|---|---|
| 1 | Phone 1, resident | Start a request, fill in the details, select Review, then Get ticket. Keep the ticket page open. | A ticket number appears with Waiting status. | | Blocked |
| 2 | Laptop, secretary | Sign in, select Call next, open the test ticket, record payment if required, then select Forward to Captain. | The test ticket goes to For signature. | | Blocked |
| 3 | Phone 2, captain | Sign in, find the test ticket under For signature, select View, then Sign. | The ticket moves to Ready for release. | | Blocked |
| 4 | Laptop, secretary | Open Ready for release, find the same ticket, select Release, and confirm. | The release succeeds. | | Blocked |
| 5 | Phone 1, resident | Return to the original ticket page. Refresh if necessary. | The same ticket shows Released. | | Blocked |

Change each Blocked to Pass or Fail and write what happened. Take a desktop and a phone screenshot for the PR. Keep passwords and private ticket links out of the screenshots.

## 4. Problems found

(None recorded yet. Write any bug here with the step number, what you expected, and what you saw.)
