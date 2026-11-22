# PilaGo staff-side defense Q&A

Plain-language answers for the staff and captain side. Where a feature is on a branch that is not merged yet, the answer says so.

## What is your part?

I built the secretary and captain screens and the staff login. My part connects the buttons staff press to the rules that decide who may act and what may happen to a ticket.

## What can each role do?

The secretary calls residents, records payment, forwards requests to the captain, puts requests on hold, and releases signed documents. The captain views and signs requests. There are two checks. First, Spring Security stops a role from opening the wrong pages: a captain opening /staff gets 403 (access denied). Second, each action asks the signed-in staff object whether it is allowed.

## How does the second check work? (polymorphism)

StaffMember is an abstract class. BarangaySecretary and BarangayCaptain each list their own allowed actions. When the code asks canPerform(action), a secretary and a captain give different answers, even though the question is the same. So the rules sit in the model, not scattered in the page code.

## What happens when a ticket changes status?

The normal flow is Waiting → Processing → For signature → Ready → Released. The enum RequestStatus lists the allowed moves in canTransitionTo. Request.updateStatus checks that list before changing anything. For example, trying to release a Waiting ticket throws InvalidStatusTransitionException. Forwarding a ticket whose fee is not paid also throws it.

## Why keep the rules in an enum?

All allowed moves are in one place. A new status or move is one edit, and a unit test can check every move.

## How are passwords stored?

Only as a BCrypt hash. The real password is never saved. At login, the typed password is hashed and compared.

## How does the login lock work? 

LoginAttempts counts consecutive wrong passwords separately for each Staff ID. The fifth failure locks that ID for ten minutes. StaffIdLockedException lets the login page show the bilingual warning. LoginAttempts uses the app Clock, so the test can move time forward ten minutes without waiting.

## Where did you use encapsulation?

In LoginAttempts, the failure count and the lock time are private. Other code can only ask the class to record a failure or to check if an ID is locked. The class alone decides when the count resets and when the lock ends.

## How does the CSV keep a name with commas in one column? 

The report loops through today's tickets and writes one row each. Text fields are wrapped in quotes, so Santos, Ana R. stays in one column. Limit to admit: the release time is taken from the ticket's last update time for released tickets, and is blank for the others.

## How did you test it?

MockMvc sends fake web requests with different test roles and checks the answers. For example, the secretary gets the report download and the captain gets 403. A local browser run followed one resident ticket from Waiting to Released. The three-device Render run is blocked until we have a working link.

## What do P1 and P2 add? 

P1 lets the captain send a request back from For signature to On hold with a reason the resident can see. It needs one new allowed move in RequestStatus, a returnToSecretary method in the queue, and a new action only the captain has. P2 records which staff member called, forwarded, and signed each request, and when. Those fields are private in Request and have no setters, which is encapsulation.

## What do the return-to-secretary and processed-by features add?

The captain can send a request back from For signature to On hold, with a reason the resident sees on their ticket. This adds one allowed move in RequestStatus, a returnToSecretary method in the queue, and an action only the captain has.

The app records which staff member called, forwarded, and signed each request, and when. These fields are private in Request and have no setters, so only the request's own methods can fill them in. That is encapsulation.

## What would you improve next?

Run the full flow on the real Render link from phones, finish P1 and P2, and add more tests for the captain pages.
