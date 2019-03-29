# TriggerRepro
Repro for several issues with Derby DB

A new and old database are provided with the repo. 10.8.1.2 was the last version of the database where this repro works out of the box. 10.14.1.0 is just there as a benchmark to show that the problem still exists.

## Summary of Issue
**Having multiple triggers with `'derby.storage.rowLocking', 'false'` causes issues with deadlocking**

1. Executing Update trigger causes deadlock when there is an Insert or Delete trigger
2. Executing Insert trigger causes deadlock when there is a Delete trigger
3. Executing Delete trigger does not cause deadlock

**`'derby.locks.deadlockTimeout'` does not seem to work in above case**

1. While executing issue above, none of the triggers were terminated and waitTimeout time was hit

## Tests performed
Each test starts as if the code was just pulled from the repo. Other instructions provided when appropriate

1. Run Database without database directory present
   * Result:
     * Insert time 0 seconds
     * Update time 0 seconds
     
The following tests were performed with the database directory generated from previous test present
     
2. Run Database with database directory present
   * Result:
     * Insert time 15 seconds
     * Update time 15 seconds
     
3. Comment out "INSERT" command from `updateDatabase()`
   * Result:
     * Insert time 0 seconds
     * Update time 15 seconds
     
4. Comment out "UPDATE" command from `updateDatabase()`
   * Result:
     * Insert time 15 seconds
     * Update time 0 seconds
     
5. Comment out Update trigger from `setDatabaseTriggers()`
   * Result:
     * Insert time 15 seconds
     * Update time 0 seconds
     
6. Comment out Insert trigger from `setDatabaseTriggers()`
   * Result:
     * Insert time 0 seconds
     * Update time 15 seconds
     
7. Comment out Delete trigger from `setDatabaseTriggers()`
   * Result:
     * Insert time 0 seconds
     * Update time 15 seconds

8. Comment out Update and Delete triggers from `setDatabaseTriggers()`
   * Result:
     * Insert time 0 seconds
     * Update time 0 seconds
     
9. Comment out Insert and Delete triggers from `setDatabaseTriggers()`
   * Result:
     * Insert time 0 seconds
     * Update time 0 seconds
     
10. Comment out Update, Insert, and Delete triggers from `setDatabaseTriggers()`
    * Result:
      * Insert time 0 seconds
      * Update time 0 seconds
      
11. Delete database directory. Move Insert trigger code block above Update trigger code block in `setDatabaseTriggers()`
    * Result:
      * Insert time 0 seconds
      * Update time 0 seconds
     
The following tests were performed with the database directory generated from previous test present.
The following tests were performed with Insert trigger code block above Update trigger code block

12. Run Database with database directory present
    * Result:
      * Insert time 15 seconds
      * Update time 15 seconds
      
13. Comment out Update trigger from `setDatabaseTriggers()`
    * Result:
      * Insert time 15 seconds
      * Update time 0 seconds
      
## Conclusions Based on Results
**Having multiple triggers with `'derby.storage.rowLocking', 'false'` causes issues with deadlocking**

Triggers seem to get into deadlock scenarios with any trigger defined after itself. As seen with test 2-13 specifically test 11, you can see that triggers are effected by those that are defined afterwards. If this is the case, it should be documented [somewhere](https://db.apache.org/derby/docs/10.15/ref/rrefsqlj43125.html) that rowLocking needs to be enabled to use the trigger feature if multiple triggers would be used on the same database table.

**`'derby.locks.deadlockTimeout'` does not seem to work in above case**

Based on [documentation](https://db.apache.org/derby/docs/10.15/ref/rrefproper10607.html#rrefproper10607), I could not find any concrete evidence of whether this is intended functionality.
