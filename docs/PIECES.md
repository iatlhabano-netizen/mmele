# Piece sets

Every piece is drawn procedurally with Compose `Canvas` (`app/.../ui/pieces/PieceDraw.kt`), with
no bitmaps or photos. Each cow has a stable id, and that id seeds its small variations (diamond
facets, egg speckles), so a piece looks the same for the whole game and on both phones.

Each side keeps a clearly different tone so Pass & Play and online games stay readable at a
glance. `PieceStyle.contrastOk()` enforces a luminance gap, and a unit test checks every dark/light
pairing the picker allows.

| Set | Player 1 (moves first) | Player 2 | Notes |
| --- | --- | --- | --- |
| Classic pebbles | Obsidian | River quartz | The original M1 look, unchanged. |
| Rough diamonds | Smoky / yellow rough | Clear rough | Irregular 6–8-sided crystal outline with seeded vertex jitter and facet shading, plus a size jitter within about 12% so points stay readable. Clear stones get faint rainbow glints. Botswana is one of the world's leading diamond producers by value. The look is stylised, not a mineral simulation. |
| Bird eggs | Kori bustard or Crowned lapwing (darker) | Ostrich, Helmeted guineafowl or Lilac-breasted roller (lighter) | Real egg profile (narrow top, blunt base). Markings follow published descriptions (below). |

In the HUD, Pass & Play labels use the piece name ("Kori" vs "Ostrich"). Solo shows "You" vs
"Computer · Hard". Online shows "You · Ostrich" vs "Opponent · Kori". In an online room the
host's piece choice is stored in the room, so both phones draw the same set.

## Bird eggs: sources

All five birds occur in Botswana. Egg appearance is drawn only from these descriptions:

| Egg | How it's drawn | Source |
| --- | --- | --- |
| **Ostrich** (*Struthio camelus*) | Glossy cream, with many small dark pits. | Wikipedia, *Common ostrich*: "glossy cream-coloured, with thick shells marked by small pits". https://en.wikipedia.org/wiki/Common_ostrich |
| **Helmeted guineafowl** (*Numida meleagris*) | Pale buff to white, densely speckled, with a heavier rim to suggest the thick shell. | BDI (adapted from SABAP1): "pale brown to white with speckles and are among the thickest-shelled of all eggs relative to size". https://thebdi.org/2023/11/08/helmeted-guineafowl-numida-meleagris/ |
| **Crowned lapwing** (*Vanellus coronatus*) | Yellowish to olive-brown base, bold black and grey blotches. | BDI: "yellowish-brown to olive-brown, boldly spotted with black and grey". https://thebdi.org/2023/07/07/crowned-lapwing-vanellus-coronatus/ · Botswana range: https://www.biodiversityexplorer.info/birds/charadriidae/vanellus_coronatus.htm |
| **Lilac-breasted roller** (*Coracias caudatus*) | Plain glossy white, no markings (it nests in tree cavities). | Kruger Park guide: "They lay 2-4 white eggs". https://www.krugerpark.co.za/africa_lilac_breasted_roller.html · Earth Life: "smooth and white". https://earthlife.net/lilac-breasted-rollers/ · Nests in tree cavities, common in Botswana: https://www.biodiversityexplorer.info/birds/coraciidae/coracias_caudatus.htm |
| **Kori bustard** (*Ardeotis kori*) | Pale olive-brown with darker olive-green and brown streaks and blotches. | BDI (adapted from SABAP1): "pale olive-brown with darker olive-green and brown streaks and shading". Largest populations in Namibia and Botswana. Tswana name *Kgôri*. https://thebdi.org/2024/06/12/kori-bustard-ardeotis-kori/ |

Left out on purpose: species where I couldn't find a clear egg description to cite.

Egg sizes are not to scale. An ostrich egg is about 15 cm long, but on the board every egg is
the same size so it fits a point.
