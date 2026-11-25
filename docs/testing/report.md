# PilaGo P5 test report

- Tester: Precious
- Where: the live Render site, https://pilago.onrender.com
- When: 25 November 2026, about 4:00 to 5:00 AM, Philippine time
- Scope: resident → secretary → captain → secretary release, plus rules, security, report and phone look
- Overall result: Not finished.Problems were found (section 3). The fixes are waiting for review, so the retest in section 4 is still to do.

## 1. Devices used

- Phone 1 (resident): Tecno, HiOS, Chrome, mobile data
- Phone 2 (captain): Tecno, HiOS, Chrome, Wi-Fi
- Laptop A (secretary): Dell, Windows 11, Chrome
- Laptop B (display board): Asus TUF, Windows 11, Brave

Accounts used: the team's staff accounts (SEC-001 and CAP-001) and made-up resident names. No passwords are written in this report.

## 2. Results so far

A. Can everyone open it?
- T1, site opens on a phone over mobile data: Pass. The first load took 93 seconds because Render's free plan puts the site to sleep when nobody uses it. Open the site a few minutes before a demo.
- T2, display board opens: result not written down yet.
- T3, both staff can log in: Pass. The secretary reached the dashboard and the captain reached For signature.

B. The main flow
- T4 to T6: results not written down yet.
- T7, secretary processes and forwards: **Fail** (problem 1 below).
- T8, captain's For signature list: **Fail** (problem 2 below).
- T9, secretary's Ready for release list: **Fail** (problem 3 below).

C to F (T10 to T27): not run or not written down yet. Fill these in from the test sheet before submitting.

Earlier local checks (localhost, 23 November):
- Resident → secretary → captain → release in separate browser sessions: Pass.
- Login lock after 5 wrong passwords: Pass, and the automated test covers the 5th failure and the unlock after 10 minutes. Now merged.
- CSV report: downloads, a name with a comma stays in one column. Now merged. Excel or Google Sheets check: still to do.

Automated tests: run `.\mvnw.cmd verify` in the PilaGo folder and write the result here: ____ tests, ____ failures.

## 3. Problems found

**1. T7: Record payment**
Expected: after pressing Record payment, the page stays where I was, and a payment recorded by mistake can be undone. What happened: the page reloaded and went back to the top, and there was no way to undo the recorded payment.
Status: fixed in `feature/staff-fixes` (Undo payment button, and the page returns to the payment box), waiting for review.

**2. T8: Captain's For signature list**
Expected: Senior and PWD tickets are at the top of the list. What happened: a regular ticket appeared above a Senior or PWD ticket.
Status: fixed in `feature/staff-fixes` (Senior and PWD first, then by ticket number), waiting for review.

**3. T9: Secretary's Ready for release list**
Expected: Senior and PWD tickets are at the top of the list. What happened: the same problem; the list followed the order the tickets were issued.
Status: fixed in `feature/staff-fixes` (same change as problem 2), waiting for review.

## 4. Retest after the fixes are merged and deployed

Repeat these on Render, then change the result above to Pass or Fail:
- T7: record a payment, then undo it; the page stays at the payment box.
- T8 and T9: forward or sign one regular ticket and one Senior or PWD ticket; the Senior or PWD ticket is on top.
- T18: the captain returns a request with a reason, and with "Other" and an empty note (it must be refused).

Extra tests from the QA review (add these to the sheet as T28 onward):
- Two secretary windows on the same ticket: forward in one, then press Pay or Forward in the other. Expect a red message, no crash.
- Double tap on Get ticket, Forward and Release. Expect no second ticket and no crash.
- Three phones submit a ticket at the same moment. Expect three different numbers.
- Full return loop: forward, captain returns, Verify, call, forward, sign, release. Payment is not asked for again.
- Skip a ticket, then the same resident requests again. A new ticket is allowed.
- Boundaries: age 59 against 60 (S- starts at 60), and 5 months against 6 months of residency.
- Call next with an empty queue. Expect a friendly message.
- Logout, then press Back, then open /staff. Expect the login page.
- Captain phone idle for 30 minutes, then tap Sign. Expect the login page.
- Display board with Wi-Fi off for 30 seconds, then on. It catches up without a refresh.
- Lock Phone 1 for a minute after taking a ticket, then call it. The alert is seen after unlocking.
- Phone at 320 px width and in landscape. No cut-off text or overlapping buttons.
- Report with a name like `Peñaflor, "Nene" Q.`. It stays in one column and opens correctly in Excel.

## 5. Known limits 
- Anyone can lock SEC-001 or CAP-001 for 10 minutes by typing 5 wrong passwords on the public login page. Do not test the lock on the real staff IDs; use a made-up ID such as TEST-999.
- The duplicate check compares names only, so "Ana  Santos" with two spaces gets past it, and two different residents with the same name block each other.
- Tickets not finished by midnight disappear from the staff screens, because the app loads only today's tickets.
- The first visit to the Render site can take over a minute.
