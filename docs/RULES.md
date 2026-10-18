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
is asserted on every ply of 3,000 random classic games, 5,000 random triangle games and 3,000 slide-only triangle games (`FuzzTest`).

## Khutlotharo (triangle)

**The layout comes from the user, a Motswana who plays the game, so it is no longer a reconstruction.** The user
sent a photo of a hand-drawn board. The photo is rotated with the apex pointing left; turned upright, it is a
tall isosceles triangle with:

```
            A
           /|\
          / | \
        CL--C--CR
        /   |   \
       /    |    \
     BL-----BM-----BR
```

* **7 points**: apex `A`; crossbar `CL`, `C`, `CR` at half height; base `BL`, `BM`, `BR`.
* **10 links**: A–CL, A–C, A–CR, CL–C, C–CR, CL–BL, C–BM, CR–BR, BL–BM, BM–BR.
* **5 lines of three**: A–CL–BL and A–CR–BR (the sides), A–C–BM (the bisector), CL–C–CR (the crossbar),
  BL–BM–BR (the base).

This is the same board as Zimbabwe's **Tsoro Yematatu** (sources 4 and 5). In the photo the centre point `C` is
drawn as a filled dot rather than a ring. Following the user's description, it is treated as an ordinary playable
point.

### Play

* **3 cows each**, Dark first. Three is the only count that works on this board: with 2 you can never make a
  line, and with 4 each the 8 cows don't fit on 7 points.
* **Placement**: take turns placing until all 6 cows are down (one point stays empty).
* **Movement**: on your turn, move one cow to the empty point. You can either
  * **slide** one step along a line to an adjacent empty point, or
  * **jump**: from the end of a line, over the occupied middle point (either colour), to the empty end. Nothing
    is captured.
  There is no flying and no shooting.
* **Win**: get three in a line on one of the 5 lines, during placement *or* movement. A player with **no legal
  move** also loses.
* **Draw**: 30 plies (15 moves each) in the movement phase without a result.

### Why jumps are on

The brief said to slide only unless the sources said otherwise. Both written Tsoro Yematatu sources (Wikipedia,
and Ludii after Matthews 1964) allow the jump over one adjacent piece of either colour into an empty point, so
Mmele follows them. I found no written source for Khutlotharo itself, but the user (a Motswana who plays the game) confirmed on 2026-10-09 that jumps are part of Khutlotharo. Jumps are controlled by one flag,
`RuleSet.TRIANGLE.jumpOverAdjacent`. Set it to `false` for slide-only, and the engine, AI and tests support
both.

One side effect: with jumps, a full 3-against-3 board always leaves the player to move at least one move into
the empty point. So the *no legal move loses* rule never fires in this variant. It is kept for the slide-only
setting, where blockades do happen. `FuzzTest` checks both.

### Solved (`tools/solve_triangle.py`)

Exhaustive retrograde analysis of every reachable position:

| Setting | Reachable positions | Perfect play | Longest forced win | Notes |
|---|---|---|---|---|
| With jumps (as shipped) | 815 | **Draw** | 3 plies | Every first placement draws. |
| Slide only (`--jumps` off) | 804 | **Draw** | 11 plies (7 in movement) | A first cow on CL, C or CR **loses** for Dark; A, BL, BM, BR draw. |

So neither player can force a win. The 30-ply draw rule is longer than any forced win (11 plies), so it never
cuts a won game short.

### Still assumed (not from the user or a source)

1. **Jumping** is confirmed by the user (2026-10-09) and matches the Tsoro Yematatu sources.
2. **Who starts**: Dark (player 1) always places first.
3. **Draw rule**: 30 movement plies without a line. Tradition probably just stops the game by agreement.
4. **Repetition**: the 30-ply rule is the only repetition rule.
5. **No legal move loses** is kept from Morabaraba (it only matters when jumps are off).

## Phase state machine

```
PLACEMENT ──(all own cows placed)──▶ MOVEMENT ──(down to 3 cows)──▶ FLYING
     │                                   │                            │
     └───── mill ──▶ [MUST SHOOT] ◀──────┴────────────────────────────┘
any state ──(opponent ≤ 2 cows | opponent has no legal move | draw counter)──▶ GAME_OVER
```
Phases are derived per player from immutable state (cows in hand / on board), so the state machine cannot get
out of sync with the position.

## Moruba (Nsolo), four-row mancala

Implemented in `engine/.../moruba/MorubaEngine.kt` (rules), `MorubaAi.kt` (AI) and
`app/.../ui/moruba/BoardScreen.kt` (board). Modes: vs Computer (Easy / Medium / Hard) and Pass & Play.

- **Board:** 4 rows of 6, 8 (default) or 10 pits. North (top, player 2) owns rows 0 (outer) and 1 (inner);
  South (bottom, player 1, you) owns rows 2 (inner) and 3 (outer). 2 stones per pit by default (2–4 selectable).
- **Sowing:** pick up all stones from one of your pits holding **at least 2** and sow one per pit,
  **counter-clockwise** around your own 16-pit loop (with 8 holes). Stones never enter the opponent's pits.
- **Relay:** if the last stone lands in an occupied pit, lift that whole pit and keep sowing.
- **Capture:** if the last stone lands in an **empty inner** pit and the opponent's facing inner pit has
  stones, take those stones **and** the stones in the opponent's outer pit behind it (front-and-back
  capture). The turn ends. Captured stones leave the board.
- **Empty outer pit:** the turn simply ends.
- **Winning:** the player to move loses when they have no stones or no pit with 2 or more stones (no legal move).
- **Step cap (infinite relays):** relay chains can in theory cycle. A turn stops after
  `MorubaConfig.stepCap` stone drops (default 2,000, far beyond any real turn). Stones still in hand are put
  into the pit where the relay stopped (so no stone is lost) and the turn passes. Tests force a cap of 3 to
  check this.
- **AI:** Easy = random legal move; Medium = greedy (largest capture, then the longest sowing);
  Hard = negamax alpha-beta (depth 4) scoring board presence, mobility (pits with ≥ 2) and capture threat,
  so a position that leaves you open to a big inner+outer double capture scores badly.

Sources: Wikipedia, "Moruba (game)" and "Mancala"; Russ, *The Complete Mancala Games Book*. Local rule
details differ from village to village; these are the defaults asked for by the developer.
