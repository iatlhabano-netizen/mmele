"""Exhaustive retrograde analysis of the reconstructed triangle variant.
Nodes: 0 apex; rows r=1..3 each (L,C,R) -> idx 1+3*(r-1)+{0,1,2}.
Lines: left edge, right edge, bisector (4 nodes each), 3 crossbars (3 nodes).
Win: 3 contiguous own tokens on a line. Lose: no legal move on your turn.
"""
import sys, itertools
from functools import lru_cache
JUMPS = '--jumps' in sys.argv
TOK = int(sys.argv[1]) if len(sys.argv) > 1 and sys.argv[1].isdigit() else 3
def n(r,c): return 0 if r==0 else 1+3*(r-1)+c
lines = [[0,n(1,0),n(2,0),n(3,0)],[0,n(1,2),n(2,2),n(3,2)],[0,n(1,1),n(2,1),n(3,1)]]+[[n(r,0),n(r,1),n(r,2)] for r in (1,2,3)]
wins=set(); adj={i:set() for i in range(10)}; jumps={i:set() for i in range(10)}
for L in lines:
    for i in range(len(L)-2): wins.add(tuple(sorted(L[i:i+3])))
    for i in range(len(L)-1): adj[L[i]].add(L[i+1]); adj[L[i+1]].add(L[i])
    for i in range(len(L)-2): jumps[L[i]].add((L[i+1],L[i+2])); jumps[L[i+2]].add((L[i+1],L[i]))
def won(b,p): return any(all(b[x]==p for x in w) for w in wins)
# state: (board tuple, toMove, hand1, hand2)
states=[]
def succ(s):
    b,p,h1,h2=s; hand = h1 if p==1 else h2; out=[]
    if hand>0:
        for i in range(10):
            if b[i]==0:
                nb=list(b); nb[i]=p
                out.append((tuple(nb),3-p,h1-(p==1),h2-(p==2)))
    else:
        for i in range(10):
            if b[i]!=p: continue
            dests=[j for j in adj[i] if b[j]==0]
            if JUMPS: dests += [d for (o,d) in jumps[i] if b[o]!=0 and b[d]==0]
            for j in dests:
                nb=list(b); nb[i]=0; nb[j]=p
                out.append((tuple(nb),3-p,h1,h2))
    return out
# enumerate reachable
start=((0,)*10,1,TOK,TOK); seen={start}; stack=[start]; edges={}
while stack:
    s=stack.pop(); b=s[0]
    if won(b,1) or won(b,2): edges[s]=[]; continue
    ss=succ(s); edges[s]=ss
    for t in ss:
        if t not in seen: seen.add(t); stack.append(t)
# value from perspective of player to move: 1 win, -1 loss, 0 draw/unknown
val={}; cnt={}; preds={s:[] for s in seen}
for s,ss in edges.items():
    for t in ss: preds[t].append(s)
from collections import deque
q=deque()
for s in seen:
    b,p=s[0],s[1]
    if won(b,3-p): val[s]=-1; q.append(s)   # previous mover won
    elif not edges[s]: val[s]=-1; q.append(s)  # blocked
    cnt[s]=len(edges[s])
while q:
    t=q.popleft()
    for s in preds[t]:
        if s in val: continue
        if val[t]==-1: val[s]=1; q.append(s)
        else:
            cnt[s]-=1
            if cnt[s]==0: val[s]=-1; q.append(s)
print(f"tokens={TOK} jumps={JUMPS} reachable={len(seen)} start value (P1 to move)={val.get(start,0)} [1=P1 forced win,-1=P2 forced win,0=draw]")
# how many first placements lose / win
for i in range(10):
    nb=[0]*10; nb[i]=1; t=(tuple(nb),2,TOK-1,TOK)
    print(' first placement',i,'value for P2:',val.get(t,0))
