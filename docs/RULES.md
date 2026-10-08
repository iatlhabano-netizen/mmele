# Mmele rules (as implemented)

Mmele (Setswana *mmela/mmele*) is the Botswana name for **Morabaraba**, the Southern African
twelve-cow mill game. The engine (`:engine`) implements the **Mind Sports South Africa (MSSA)
"Generally Accepted Rules" (GAR)**, which Wikipedia's article follows, plus a few explicitly
documented decisions where the sources are silent or a computer game needs a hard rule.

## Sources

1. Mind Sports South Africa, *Generally Accepted Rules for Morabaraba* —
   http://www.mindsportssa.freeservers.com/rules-morabaraba.htm (also mirrored at
   https://esportscommentator.blogspot.com/2015/04/generally-accepted-rules-for-game-of_4.html)
2. Wikipedia, *Morabaraba* — https://en.wikipedia.org/wiki/Morabaraba (names *mmela* as the Setswana name)
3. Ways To Play, *Morabaraba* — https://games.porg.es/games/morabaraba/
4. Wikipedia, *Tsoro yematatu* — https://en.wikipedia.org/wiki/Tsoro_yematatu
5. Ludii Portal, *Tsoro Yemutatu (Triangle)* (after Matthews 1964, *NADA* 9(1)) —
   https://ludii.games/details.php?keyword=Tsoro+Yemutatu+%28Triangle%29

## Classic (12 cows)

### Board
* 24 points on **three concentric squares**, joined by the **four midlines** *and* the **four corner
  diagonals** (the diagonals are what distinguish Morabaraba from Twelve Men's Morris boards drawn without them).
* 40 links. Outer and inner ring points have 3 links; middle ring points have 4.
* **20 mills**: 12 square sides + 4 midlines + 4 diagonals. Any straight line of three linked points
  is a mill (tested exhaustively in `BoardGraphTest.allTwentyMillsAndOnlyThose`).
* Notation: files a–g left→right, ranks 1–7 bottom→top (standard morris notation).

### Play
* Each player has **12 cows**. **Dark (Obsidian) moves first** (Wikipedia; MSSA does not specify a colour).
* **Placing (Go baya)**: alternately place one cow on any empty point.
* **Moving (Go sutisa)**: once *all* of a player's cows are placed, a turn is moving one cow to an
  adjacent empty point along a line.
* **Flying (Go fofa)**: a player with all cows placed and **exactly three** cows left may move a cow to
  *any* empty point. Only the player on three flies.
* **Mills / shooting**: forming a mill (by placing or moving) puts the mover in the *must shoot*
  sub-state: they remove one enemy cow. A cow that is in a mill is **protected**, unless **every** enemy
  cow is in a mill, in which case any cow may be shot.
* Mills may be broken and re-formed; every re-formation shoots again.
* **MSSA re-mill rule** (implemented, on by default): if a move *breaks* a mill and *immediately forms* a
  new one, that cow may not move straight back to its original point on the player's next move **if that
  move would form a mill**. (Another cow may still enter that point.)

### Winning
* A player **wins** when the opponent is **reduced to two cows** (board + hand), or when the opponent
  **cannot make a legal move** on their turn (*Go tswala*, a blockade).

## Edge cases — decisions

| Situation | Decision | Why |
|---|---|---|
| A single placement/move forms **two mills at once** | **One shot only.** | Explicit in MSSA GAR ("only one of the opponent's cows may be shot at a time, even if two (or more) lines … are achieved with one move"), Wikipedia and Ways To Play. |
| A mill is formed while the opponent has **no cow on the board** (only possible in the first plies) | No shot; turn passes. | Nothing to shoot; avoids a dead "must shoot" state. |
| All enemy cows are in mills | Any enemy cow may be shot. | GAR. |
| **Full board** after placement (24 cows, no mill ever formed) | The player to move (Dark, who moves first in the movement phase) **has no legal move and loses** (blockade). | Direct consequence of GAR's "the game is won when the opponent cannot move". Some casual players call this a draw; we follow the written rule. The game never deadlocks: the state is terminal (`RulesTest.fullBoardAtEndOfPlacementIsALossForTheBlockedMover`). |
| Re-mill ban would remove a player's **only** legal moves | The banned move becomes legal again. | A rule should never create a blockade loss by itself. In practice this can't happen (the other cows of the mill can always step into that point), but the guard keeps the "never zero legal actions" invariant airtight. |
| A player is down to 3 cows and nobody shoots | **Draw after 10 moves each (20 plies) without a shot** while at least one player is flying. | GAR: "When a player is down to three cows, and neither player is able to shoot … within ten moves, the game is a draw." We read "ten moves" as ten per player. |
| Endless shuffling with more than 3 cows each | **Draw after 50 moves each (100 plies) with no placement and no shot.** | Not in GAR; added (like chess's 50-move rule) so every game provably terminates. Placements (≤24) and shots (≤20) are finite and each resets the counter. |
| Flying player vs. sliding player | Only the player on three flies. | Wikipedia. |
| Who loses first if both conditions hit | After every turn the engine checks, for the player now to move: ≤2 cows → loss; no legal move → loss; then the draw counters. | Deterministic ordering. |

**No-deadlock guarantee.** The engine's invariant — *every non-terminal state has at least one legal action* —
is asserted on every ply of 3,000 random classic games and 5,000 random triangle games (`FuzzTest`).

## Khutlotharo (triangle) — a reconstruction

**Please read this as a design, not as documented tradition.** I could not find any source for a
Setswana triangle game called *Khutlotharo* (Setswana for "triangle"). The closest documented relative is
**Tsoro Yematatu** ("three-stone game", Zimbabwe; recorded by Matthews 1964): an isosceles triangle with a
central bisector and one crossbar (7 points), 3 pieces each, place then move (adjacent step, or a jump over one
piece), first to line up three wins. Related small alignment games include Tapatan, Achi and Shisima.

To match the spec (triangle, central bisector, horizontal crossbars, ~10 nodes, 3–4 tokens, sudden death),
Mmele uses this version:

* **Board**: an apex plus **three crossbars** of three points each (left side, centre, right side): **10 points**.
  Lines: the two sides and the bisector (4 points each) and the three crossbars (3 points each).
  15 links; **9 winning lines** (any three *consecutive* points on one line).
* **4 cows each**, Dark first. Place alternately (8 of the 10 points get filled), then move one cow to an
  adjacent empty point along a line. No jumping, no flying, no shooting.
* **Win**: make three in a line (during placement *or* movement) — or leave the opponent with **no legal move**.
* **Draw**: 20 moves each (40 plies) in the movement phase without a result.

Design check: an exhaustive retrograde analysis (`tools/solve_triangle.py`, ~16k reachable positions) shows this
variant is a **draw with perfect play** — neither player has a forced win, so it is not a trivially solved
first-player win. The same holds for 3 cows, and with Tsoro-style jumps.

## Phase state machine

```
PLACEMENT ──(all own cows placed)──▶ MOVEMENT ──(down to 3 cows)──▶ FLYING
     │                                   │                            │
     └───── mill ──▶ [MUST SHOOT] ◀──────┴────────────────────────────┘
any state ──(opponent ≤ 2 cows | opponent has no legal move | draw counter)──▶ GAME_OVER
```
Phases are derived per player from immutable state (cows in hand / on board), so the state machine cannot get
out of sync with the position.
