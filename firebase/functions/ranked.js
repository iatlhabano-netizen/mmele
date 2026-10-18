// Pure settlement + bracket logic shared by the Cloud Functions and their tests.
// Mirrors online/src/main/kotlin/bw/mmele/online/ranked/{Ranked,Tournament}.kt (RankedTest checks the
// same vectors as ranked.test.mjs). Virtual points only: no cash value, never bought or cashed out.
export const START_RATING = 1200, START_POINTS = 1000, STAKE = 10, K_NEW = 40, K = 24, ENTRY = 20;

export const fresh = (uid) => ({ uid, rating: START_RATING, points: START_POINTS, games: 0, wins: 0 });
const expected = (a, b) => 1 / (1 + Math.pow(10, (b - a) / 400));
// Kotlin Math.round rounds half up (floor(x + 0.5)), same as JS Math.round.
export function eloUpdate(a, b, scoreA, gamesA = 30, gamesB = 30) {
  const ka = gamesA < 30 ? K_NEW : K, kb = gamesB < 30 ? K_NEW : K;
  const ea = expected(a, b);
  return [Math.round(a + ka * (scoreA - ea)), Math.round(b + kb * ((1 - scoreA) - (1 - ea)))];
}

export function settleMatch(one, two, winner, stake = STAKE) {
  const score = { one: 1, two: 0, draw: 0.5 }[winner];
  if (score === undefined) throw new Error("bad winner " + winner);
  const [r1, r2] = eloUpdate(one.rating, two.rating, score, one.games, two.games);
  const moved = winner === "one" ? Math.min(stake, two.points) : winner === "two" ? Math.min(stake, one.points) : 0;
  const n1 = { ...one, rating: r1, points: one.points + (winner === "one" ? moved : winner === "two" ? -moved : 0), games: one.games + 1, wins: one.wins + (winner === "one" ? 1 : 0) };
  const n2 = { ...two, rating: r2, points: two.points + (winner === "two" ? moved : winner === "one" ? -moved : 0), games: two.games + 1, wins: two.wins + (winner === "two" ? 1 : 0) };
  return { one: n1, two: n2, moved };
}

/** Winner of a finished ranked room: a rules-validated resignation/claim decides alone; otherwise both reports must agree. */
export function decideWinner(room, results) {
  if (room.resigned === "one") return "two";
  if (room.resigned === "two") return "one";
  const a = results?.one, b = results?.two;
  if (a && b) return a === b ? a : "disputed";
  return null; // wait for the other report
}

export const SIZES = [4, 8, 16];
export function createBracket(players) {
  const n = players.length;
  if (!SIZES.includes(n)) throw new Error("bad size");
  const rounds = Math.log2(n), m = {};
  for (let i = 0; i < n / 2; i++) m[`r1m${i}`] = { round: 1, index: i, one: players[2 * i], two: players[2 * i + 1] };
  for (let r = 2; r <= rounds; r++) for (let i = 0; i < n >> r; i++) m[`r${r}m${i}`] = { round: r, index: i };
  return m;
}
export function reportBracket(matches, size, id, winner) {
  const rounds = Math.log2(size), m = matches[id];
  if (m.winner) throw new Error("decided");
  if (winner !== m.one && winner !== m.two) throw new Error("not in match");
  const out = JSON.parse(JSON.stringify(matches));
  out[id].winner = winner;
  if (m.round < rounds) { const nx = `r${m.round + 1}m${Math.floor(m.index / 2)}`; out[nx][m.index % 2 === 0 ? "one" : "two"] = winner; }
  return out;
}
export const championOf = (matches, size) => matches[`r${Math.log2(size)}m0`]?.winner ?? null;
export const prizes = (size, entry = ENTRY) => { const pot = size * entry, first = Math.floor(pot * 7 / 10); return [first, pot - first]; };
export const shuffle = (a, rnd = Math.random) => { a = [...a]; for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(rnd() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };
