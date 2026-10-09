"""Exhaustive retrograde analysis of Khutlotharo (7-point triangle, same board as Tsoro Yematatu).

Board (apex up), indices:      0 A
                         1 CL   2 C   3 CR
                     4 BL       5 BM       6 BR
Lines of three (the only wins): A-CL-BL, A-CR-BR, A-C-BM, CL-C-CR, BL-BM-BR.
Adjacency: consecutive points on those lines.
Rules solved: drop TOK cows each alternately, then move one cow per turn to an adjacent empty
point (and, with --jumps, over an adjacent cow of either colour to the empty point directly
beyond it on the same line, no capture). Three in a line wins (placement or movement). A player
with no legal move on their turn loses. Positions that neither side can force are draws.

Usage: python3 tools/solve_triangle.py [TOK] [--jumps]
Prints the value of the start position, the longest forced win (in plies, from any won
position), and the value of each first placement.
"""
import sys
from collections import deque

JUMPS = '--jumps' in sys.argv
TOK = next((int(a) for a in sys.argv[1:] if a.isdigit()), 3)
N = 7
NAMES = ['A', 'CL', 'C', 'CR', 'BL', 'BM', 'BR']
LINES = [(0, 1, 4), (0, 3, 6), (0, 2, 5), (1, 2, 3), (4, 5, 6)]
adj = {i: set() for i in range(N)}
jumps = {i: set() for i in range(N)}
for a, b, c in LINES:
    adj[a] |= {b}; adj[b] |= {a, c}; adj[c] |= {b}
    jumps[a].add((b, c)); jumps[c].add((b, a))

def won(b, p):
    return any(all(b[x] == p for x in L) for L in LINES)

def succ(s):
    b, p, h1, h2 = s
    hand = h1 if p == 1 else h2
    out = []
    if hand > 0:
        for i in range(N):
            if b[i] == 0:
                nb = list(b); nb[i] = p
                out.append((tuple(nb), 3 - p, h1 - (p == 1), h2 - (p == 2)))
        return out
    for i in range(N):
        if b[i] != p:
            continue
        dests = [j for j in adj[i] if b[j] == 0]
        if JUMPS:
            dests += [d for (o, d) in jumps[i] if b[o] != 0 and b[d] == 0]
        for j in dests:
            nb = list(b); nb[i] = 0; nb[j] = p
            out.append((tuple(nb), 3 - p, h1, h2))
    return out

start = ((0,) * N, 1, TOK, TOK)
seen = {start}; stack = [start]; edges = {}
while stack:
    s = stack.pop()
    if won(s[0], 1) or won(s[0], 2):
        edges[s] = []; continue
    edges[s] = succ(s)
    for t in edges[s]:
        if t not in seen:
            seen.add(t); stack.append(t)

preds = {s: [] for s in seen}
for s, ss in edges.items():
    for t in ss:
        preds[t].append(s)
val, depth, cnt = {}, {}, {}
q = deque()
for s in seen:
    cnt[s] = len(edges[s])
    if won(s[0], 3 - s[1]) or not edges[s]:
        val[s] = -1; depth[s] = 0; q.append(s)
while q:
    t = q.popleft()
    for s in preds[t]:
        if s in val:
            continue
        if val[t] == -1:
            val[s] = 1; depth[s] = depth[t] + 1; q.append(s)
        else:
            cnt[s] -= 1
            if cnt[s] == 0:
                val[s] = -1; depth[s] = depth[t] + 1; q.append(s)

word = {1: 'FIRST PLAYER WINS', -1: 'SECOND PLAYER WINS', 0: 'DRAW'}
v = val.get(start, 0)
print(f"tokens={TOK} jumps={JUMPS} reachable={len(seen)} start={word[v]} (value {v})")
print(f"longest forced win/loss line from any decided position: {max(depth.values(), default=0)} plies")
moving = [s for s in seen if s[2] == 0 and s[3] == 0 and edges[s]]
print(f"movement-phase positions: {len(moving)}, decided: {sum(1 for s in moving if s in val)}, "
      f"max forced length there: {max((depth[s] for s in moving if s in val), default=0)} plies")
for i in range(N):
    nb = [0] * N; nb[i] = 1
    t = (tuple(nb), 2, TOK - 1, TOK)
    print(f"  first cow on {NAMES[i]:>2}: {word[-val.get(t, 0)] if t in val else 'DRAW'}")
