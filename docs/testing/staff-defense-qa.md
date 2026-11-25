# PilaGo staff-side defense Q&A

Plain answers for the staff and captain side, in my own words.

## What is your part?

The staff and captain side and the security: the staff login, the secretary dashboard and process page, the captain's sign list, the role limits, the error pages and the tests for them. I also added the login lock (P3), the report download (P4), the captain's Return to secretary screen (P1), and fixes after the Render test.

Honest split: Isaac wrote the model and queue rules for P1 and P2 (the new status move, the returnToSecretary method, and the fields that remember who did each step). I built what the staff and captain actually see and press, and the tests for it.

## What can each role do?

The secretary calls residents, records payment, forwards requests to the captain, puts requests on hold, verifies, skips and releases. The captain views the For signature list, signs, and returns a request to the secretary. There are two checks. First, Spring Security stops a role from opening the wrong pages: a captain opening /staff gets 403 (access denied), and anyone not logged in goes to the login page. Second, every action asks the signed-in staff member whether that action is allowed for them.

## How does the second check work? (roles and polymorphism)

StaffMember is an abstract class. BarangaySecretary and BarangayCaptain each give their own list of allowed actions through getAllowedActions(). The code only asks canPerform(action), and the answer is different for each role, even though the question is the same. So the rules sit in the model and not scattered in the page code. The actions themselves are the values of the StaffAction enum.

## How do status changes work, and why an enum?

The normal flow is Waiting → Processing → For signature → Ready → Released. The enum RequestStatus lists the allowed moves in canTransitionTo, and Request.updateStatus checks that list before changing anything. Releasing a Waiting ticket, for example, throws InvalidStatusTransitionException. Having all the moves in one enum means a new move is one edit and a unit test can check every move.

## Which exceptions do you use?

They all extend PilaGoException, so one handler turns them into a red message and sends the user back to the page. InvalidStatusTransitionException is for a move that is not allowed. QueueBusyException is for calling the next ticket while one is still at the counter. TicketNotFoundException is for a ticket number that does not exist. ReturnNoteRequiredException is for a captain who picks "Other" without a note. AccountLockedException is for a locked Staff ID. DuplicateRequestException and NotAResidentException are on the resident side.

## What does P1 (Return to secretary) do?

The captain can send a request from For signature back to On hold, with a reason from a list (Wrong details, Payment not confirmed, ID needs checking, Other). The resident sees the reason on their ticket, and the secretary sees "Returned by the captain". Rules: the new allowed move is in RequestStatus, only the captain has the RETURN_TO_SECRETARY action, so the secretary gets 403, and "Other" needs a note. Tests cover each of these.

## What does P2 (Processed by, Signed by) do, and where is encapsulation?

Request remembers who called it, received the payment, forwarded, returned, signed and released it, and when. Those fields are private and have no setters. Only the request's own methods (call, forwardToCaptain, sign, release) fill them in, so nobody can write a wrong name from outside. The captain's details and the process page show them.

## How does the login lock work? (P3)

LoginAttempts counts wrong passwords in a row for each Staff ID. The fifth one locks that ID for ten minutes, and the login page shows the warning. The counter and the lock time are private, so other code can only report a failure, report a success, or ask if an ID is locked. That is encapsulation. AccountLockedException is the custom exception for a locked ID. The class uses the app's Clock, so a test can move time forward ten minutes without waiting. Limit to admit: anyone can lock a real staff ID by typing wrong passwords.

## How are passwords stored?

Only as a BCrypt hash. The real password is never saved. At login, the typed password is hashed and compared.

## How does the report (P4) keep a name with commas in one column?

The report loops through today's tickets and writes one line for each. Text fields are wrapped in quotes, so "Santos, Ana R." stays in one column, and a value starting with = is protected so a spreadsheet does not run it as a formula. It also records the released time and which staff member did each step.

## What did the Render test find, and what did you fix?

Three things: there was no way to undo a payment recorded by mistake, and Senior and PWD tickets were not first in the captain's For signature list and the secretary's Ready for release list. The fix adds an Undo payment button that works only while the ticket is at the counter, and sorts those two lists with Seniors and PWD first. If asked, say these fixes are on the feature/staff-fixes branch until they are merged.

## How did you test it?

MockMvc sends fake web requests as different roles and checks the answers, for example the secretary downloads the report and the captain gets 403. Unit tests check the status moves and the login counter, including a clock that moves forward ten minutes. I also ran the whole flow on the live Render site with two phones and two laptops and wrote the results in docs/testing/report.md.

## What would you improve next?

Lock by Staff ID and device together so strangers cannot lock the real staff, keep unfinished tickets from the day before, make the duplicate check smarter than name only, and add more tests for the captain pages.
