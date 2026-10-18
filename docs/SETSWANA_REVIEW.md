# Setswana translation review

Mmele ships English (default) and a **draft** Setswana (`tn`) translation. Every Setswana string
below was drafted by the developer's assistant, not by a native speaker, so **all 398 of 398
strings need review** before the Setswana UI is promoted (the high-confidence ones should be quick).

How to correct: edit `app/src/main/res/values-tn/strings.xml` or `strings_academy.xml` directly
(keep `%1$s` / `%1$d` placeholders, write an apostrophe as `\'`), change the comment above the
string to `<!-- confidence: reviewed -->`, then run `python3 tools/gen_setswana.py --doc` to rebuild
this table. `LocalizationTest` checks that every key exists with the same placeholders.

Conventions in the draft (please confirm or replace):

- Game terms as you use them: **Go baya** (place), **Go sutisa** (move), **Go fofa** (fly),
  **go tswala** (locked in), **Mmele**, **Khutlotharo**, **kgomo / dikgomo** (cow / cows).
- *Mill* = **mola** (line); *shoot* = **thunya**; *opponent* = **mophenyisani**;
  *Dark / Light* sides = **Lefifi / Lesweu**; *Academy* = **Sekolo**; *Hint* = **Thuso**.
- Place names stay as Batswana write them (Okavango, Chobe, Makgadikgadi, Tsodilo, Kgalagadi).
- Bird names (lapwing, roller) are low confidence: please supply the names you know.

Counts: 89 high, 262 medium, 47 low confidence, 0 reviewed.

| Key | English | Draft Setswana | Confidence |
|---|---|---|---|
| `nav_home` | Home | Gae | high |
| `common_cancel` | Cancel | Khansela | high |
| `common_close` | Close | Tswala | high |
| `common_back` | Back | Morago | high |
| `home_tagline` | MORABARABA · THE CATTLE GAME | MORABARABA · MOTSHAMEKO WA DIKGOMO | medium |
| `home_section_board` | Board | Boto | medium |
| `home_section_mode` | Mode | Mokgwa wa go tshameka | medium |
| `home_section_clock` | Clock per player | Nako ya motshameki mongwe le mongwe | medium |
| `home_section_pieces` | Pieces | Dikgomo | medium |
| `home_section_scene` | Scene · Botswana | Lefelo · Botswana | medium |
| `home_section_language` | Language | Puo | high |
| `variant_classic` | Classic | Ya setso | medium |
| `variant_classic_sub` | 12 cows · mills shoot | Dikgomo di le 12 · mola o a thunya | medium |
| `variant_triangle` | Khutlotharo | Khutlotharo | high |
| `variant_triangle_sub` | Triangle · 3 cows · line wins | Khutlotharo · dikgomo di le 3 · mola o a fenya | medium |
| `mode_solo` | Play vs Computer | Tshameka le khomputara | high |
| `mode_solo_sub` | Solo against the AI | O le esi kgatlhanong le khomputara | medium |
| `mode_online` | Play online | Tshameka mo inthaneteng | high |
| `mode_online_sub` | A friend over the internet · room code | Tsala mo inthaneteng · khoutu ya kamore | medium |
| `mode_online_sub_off` | Coming soon in this version | E tla tla mo kgatisong e e latelang | medium |
| `mode_pass_and_play` | Pass & Play | Fetisa o tshameke | medium |
| `mode_pass_and_play_sub` | Two players on this phone | Batshameki ba babedi mo founong e le nngwe | high |
| `difficulty_easy` | Easy | Bonolo | high |
| `difficulty_medium` | Medium | Magareng | high |
| `difficulty_hard` | Hard | Thata | high |
| `difficulty_expert` | Expert | Thata thata | medium |
| `home_play_side_first` | Play %1$s (first) | Tshameka ka %1$s (o simolola) | medium |
| `home_play_side` | Play %1$s | Tshameka ka %1$s | medium |
| `clock_off` | Off | Ga e yo | medium |
| `clock_minutes` | %1$d min | Metsotso e %1$d | medium |
| `home_sound` | Sound | Modumo | high |
| `home_haptics` | Haptics | Thoromo | medium |
| `home_play` | Tshameka · Play | Tshameka | high |
| `home_find_room` | Tshameka · Find a room | Tshameka · Batla kamore | medium |
| `home_continue` | Continue game | Tswelela ka motshameko | high |
| `home_continue_detail` | %1$s · %2$s · move %3$d | %1$s · %2$s · motsamao %3$d | medium |
| `home_academy` | Academy | Sekolo | medium |
| `home_academy_sub` | Guided puzzles · %1$d of %2$d done | Dithaelo tse di kaelwang · %1$d go %2$d di weditswe | medium |
| `home_stats` | Stats | Dipalo | medium |
| `home_how_to_play` | How to play | Ka moo go tshamekwang ka teng | high |
| `home_about` | About · privacy · licences | Ka ga · sephiri · dilaesense | medium |
| `home_footer` | Mmele (Setswana) is the Morabaraba family of mill games played across Botswana and Southern Africa. | Mmele ke lelapa la metshameko ya Morabaraba e e tshamekwang mo Botswana le mo Aforika Borwa. | medium |
| `home_dark_side` | Dark side | Lekgotla le lefifi | low |
| `home_light_side` | Light side | Lekgotla le lesweu | low |
| `home_surprise` | Surprise me | Nkgakgamatse | medium |
| `home_surprise_action` | pick a random scene | tlhopha lefelo ka tshoganetso | medium |
| `lang_system` | System | Ya founo | medium |
| `phase_place` | Placing | Go baya | high |
| `phase_move` | Moving | Go sutisa | high |
| `phase_fly` | Flying | Go fofa | high |
| `phase_over` | Game over | Motshameko o fedile | high |
| `hud_you` | You | Wena | high |
| `hud_computer` | Computer · %1$s | Khomputara · %1$s | high |
| `hud_you_side` | You · %1$s | Wena · %1$s | high |
| `hud_opponent_side` | Opponent · %1$s | Mophenyisani · %1$s | medium |
| `hud_in_hand` | In hand %1$d | Mo seatleng %1$d | medium |
| `hud_on_board` | On board %1$d | Mo botong %1$d | medium |
| `hud_shot` | Shot %1$d | Tse di thuntsweng %1$d | medium |
| `prompt_ai_thinking` | Computer is thinking… | Khomputara e a akanya… | high |
| `prompt_opponent_turn` | Opponent's turn | Ke nako ya mophenyisani | medium |
| `prompt_who` | %1$s: %2$s | %1$s: %2$s | high |
| `prompt_shoot_any` | Mill! All their cows are in mills, shoot any one | Mola! Dikgomo tsotlhe tsa gagwe di mo melaong, thunya epe fela | medium |
| `prompt_shoot` | Mill! Tap a glowing cow to shoot it | Mola! Tobetsa kgomo e e phatsimang go e thunya | medium |
| `prompt_place (one)` | Place a cow · %1$d in hand | Baya kgomo · %1$d e setse mo seatleng | medium |
| `prompt_place (other)` | Place a cow · %1$d in hand | Baya kgomo · %1$d di setse mo seatleng | medium |
| `prompt_move_to` | Move to a glowing point | Sutisetsa kwa ntlheng e e phatsimang | medium |
| `prompt_move` | Tap or drag a cow to move it | Tobetsa kgotsa goga kgomo go e sutisa | medium |
| `prompt_fly` | Three cows left, fly to any empty point | Dikgomo di setse tharo, fofela kwa ntlheng epe e e lolea | medium |
| `game_undo` | Undo | Busetsa morago | medium |
| `game_offer_draw` | Offer draw | Kopa go lekalekana | medium |
| `game_draw_offered` | Draw offered | O kopile go lekalekana | medium |
| `game_resign` | Resign | Ineela | high |
| `game_confirm` | Tap to confirm | Tobetsa go netefatsa | medium |
| `game_rematch` | Rematch | Tshameka gape | high |
| `game_waiting` | Waiting… | Re emetse… | medium |
| `game_accept_rematch` | Accept rematch | Dumela go tshameka gape | medium |
| `game_leave_title` | Leave this game? | Tlogela motshameko o? | medium |
| `game_leave_online` | Your opponent can claim the win if you don't come back within 90 seconds. | Mophenyisani a ka tsaya phenyo fa o sa boe mo metsotswaneng e le 90. | medium |
| `game_leave_saved` | Your game is saved. Pick it up again with Continue game on the home screen. | Motshameko wa gago o bolokilwe. O ka o tswelela ka Tswelela ka motshameko kwa gae. | medium |
| `game_leave` | Leave | Tlogela | high |
| `game_keep_playing` | Keep playing | Tswelela o tshameka | high |
| `result_you_win` | You win! | O fentse! | high |
| `result_computer_wins` | The computer wins | Khomputara e fentse | high |
| `result_side_wins` | %1$s wins! | %1$s o fentse! | medium |
| `result_opponent_wins` | Opponent wins | Mophenyisani o fentse | medium |
| `result_draw` | A draw | Lo lekalekane | medium |
| `reason_two` | Reduced to two cows, no more mills possible. | O setse ka dikgomo di le pedi, ga go sa kgonege go dira mola. | medium |
| `reason_blockade` | Go tswala: locked in with no legal move. | Go tswala: o tswaletswe, ga go na motsamao o o letleletsweng. | medium |
| `reason_line` | Three in a line. | Tse tharo mo moleng. | medium |
| `reason_timeout` | Out of time. | Nako e fedile. | high |
| `reason_resigned` | Resigned. | O ineetse. | medium |
| `reason_abandoned` | Left the game and did not return. | O tlogetse motshameko mme ga a ka a boa. | medium |
| `reason_flying_draw` | Ten moves each with a player on three cows and no shot. | Metsamao e le lesome mongwe le mongwe, motshameki mongwe a na le dikgomo tse tharo mme go se na thunyo. | low |
| `reason_no_line (one)` | %1$d move each without a placement. | Motsamao o le %1$d mongwe le mongwe kwa ntle ga go baya. | low |
| `reason_no_line (other)` | %1$d moves each without a placement. | Metsamao e le %1$d mongwe le mongwe kwa ntle ga go baya. | low |
| `reason_no_shot (one)` | %1$d move each without a shot. | Motsamao o le %1$d mongwe le mongwe kwa ntle ga thunyo. | low |
| `reason_no_shot (other)` | %1$d moves each without a shot. | Metsamao e le %1$d mongwe le mongwe kwa ntle ga thunyo. | low |
| `reason_agreed` | Both players agreed to a draw. | Batshameki ka bobedi ba dumetse go lekalekana. | medium |
| `result_moves` | Moves played: %1$d | Metsamao e e tshamekilweng: %1$d | medium |
| `result_view_board` | View board | Leba boto | medium |
| `online_waiting` | Waiting for opponent | Re emetse mophenyisani | medium |
| `online_connected` | Opponent connected | Mophenyisani o golagane | medium |
| `online_disconnected_countdown` | Opponent disconnected · %1$ds | Mophenyisani o kgaogane · %1$ds | medium |
| `online_disconnected` | Opponent disconnected | Mophenyisani o kgaogane | medium |
| `online_sending` | sending… | e a romelwa… | medium |
| `online_share_action` | share room code | abelana ka khoutu ya kamore | medium |
| `online_away_too_long` | Your opponent has been away too long. | Mophenyisani o nnile a seyo lobaka lo lo leele. | medium |
| `online_idle_claim` | Your opponent hasn't moved for 5 minutes. | Mophenyisani ga a ise a tshameke mo metsotsong e le 5. | medium |
| `online_idle_warning` | Your move: %1$s left before your opponent can claim the game | Ke nako ya gago: go setse %1$s pele mophenyisani a ka tsaya phenyo | medium |
| `online_claim_win` | Claim win | Tsaya phenyo | medium |
| `online_room_code` | ROOM CODE | KHOUTU YA KAMORE | medium |
| `online_send_code` | Send this code to a friend. The game starts as soon as they join. | Romela khoutu e kwa go tsala. Motshameko o simolola fa a tsena. | medium |
| `online_share_code` | Share code | Abelana ka khoutu | medium |
| `online_offers_draw` | Opponent offers a draw | Mophenyisani o kopa go lekalekana | medium |
| `online_accept` | Accept | Dumela | high |
| `online_msg_connection` | Connection problem: %1$s | Bothata jwa kgolagano: %1$s | medium |
| `online_msg_not_sent` | Not sent: %1$s | Ga e a romelwa: %1$s | medium |
| `online_msg_rejected` | Move not accepted (%1$s), board resynced | Motsamao ga o a amogelwa (%1$s), boto e tsamaisanngwa sešwa | low |
| `online_msg_corrupt` | The other phone sent a move the rules don't allow. Game stopped. | Founo e nngwe e rometse motsamao o melao e sa o letleleleng. Motshameko o emisitswe. | medium |
| `online_unknown_error` | unknown | ga go itsiwe | medium |
| `share_text` | Play Mmele (Morabaraba) with me! Open the app, tap "Play online" and enter room code %1$s | Tshameka Mmele (Morabaraba) le nna! Bula app, tobetsa "Tshameka mo inthaneteng" mme o tsenye khoutu ya kamore %1$s | medium |
| `share_title` | Share room code | Abelana ka khoutu ya kamore | medium |
| `lobby_tagline` | Tshameka le tsala · play a friend on another phone | Tshameka le tsala · tsala mo founong e nngwe | medium |
| `lobby_off_title` | Online play is coming soon | Go tshameka mo inthaneteng go tla tla | medium |
| `lobby_off_body` | Online games aren't switched on in this version of Mmele yet. You can still play the computer, or a friend on this phone with Pass & Play. Both work without the internet. | Metshameko ya inthanete ga e ise e bulwe mo kgatisong e ya Mmele. O santse o ka tshameka le khomputara, kgotsa le tsala mo founong e ka Fetisa o tshameke. Tsotlhe di dira kwa ntle ga inthanete. | medium |
| `lobby_off_back` | Back to offline play | Boela kwa motshamekong o o sa tlhokeng inthanete | low |
| `lobby_err_network` | Couldn't reach the game server. Check your internet connection and try again. | Ga re a kgona go fitlhelela seva ya motshameko. Tlhola kgolagano ya gago ya inthanete mme o leke gape. | medium |
| `lobby_err_no_room` | No game with code %1$s. Check the code with your friend. | Ga go na motshameko o o nang le khoutu %1$s. Tlhola khoutu le tsala ya gago. | medium |
| `lobby_err_full` | That game already has two players. | Motshameko oo o setse o na le batshameki ba babedi. | medium |
| `lobby_err_other` | Something went wrong. | Sengwe se tsamaile phoso. | high |
| `lobby_host` | Host a game | Simolola motshameko | medium |
| `lobby_host_classic` | Classic · 12 cows · you play %1$s and move first | Ya setso · dikgomo di le 12 · o tshameka ka %1$s mme o simolola | medium |
| `lobby_host_triangle` | Khutlotharo · triangle · you play %1$s and move first | Khutlotharo · o tshameka ka %1$s mme o simolola | medium |
| `lobby_create` | Create room | Dira kamore | medium |
| `lobby_join_title` | Join a friend | Tsena mo motshamekong wa tsala | medium |
| `lobby_code_label` | Room code | Khoutu ya kamore | medium |
| `lobby_join` | Join game | Tsena | high |
| `lobby_note` | Variant and pieces come from the host's choices. Your scene stays your own. | Mofuta le dikgomo di tlhophiwa ke yo o simololang. Lefelo la gago ke la gago. | medium |
| `a11y_board` | %1$s board | Boto ya %1$s | medium |
| `a11y_board_classic` | Mmele (Morabaraba) | Mmele (Morabaraba) | high |
| `a11y_board_triangle` | Khutlotharo (triangle) | Khutlotharo (khutlotharo) | medium |
| `a11y_point` | Point %1$s, %2$s | Ntlha %1$s, %2$s | medium |
| `a11y_empty` | empty | e lolea | medium |
| `a11y_cow` | %1$s cow | kgomo ya %1$s | medium |
| `a11y_selected` | selected | e tlhophilwe | medium |
| `a11y_valid_move` | valid move | motsamao o o letleletsweng | medium |
| `a11y_can_be_shot` | can be shot | e ka thunngwa | medium |
| `a11y_hinted` | suggested by the hint | e kaelwa ke thuso | low |
| `a11y_can_move` | can move | e ka sutisiwa | medium |
| `a11y_choose` | Choose | Tlhopha | high |
| `a11y_list_separator` | ", " | ", " | high |
| `family_pebbles` | Classic pebbles | Matlapana a setso | medium |
| `family_pebbles_blurb` | Obsidian and river-quartz stones | Matlapana a obsidian le a quartz a noka | low |
| `family_diamonds` | Rough diamonds | Ditaemane tse di sa segwang | medium |
| `family_diamonds_blurb` | Botswana's famed uncut crystals | Ditaemane tse di tumileng tsa Botswana | medium |
| `family_eggs` | Bird eggs | Mae a dinonyane | high |
| `family_eggs_blurb` | Eggs of birds found in Botswana | Mae a dinonyane tse di fitlhelwang mo Botswana | medium |
| `piece_obsidian` | Obsidian | Obsidian | medium |
| `piece_obsidian_full` | Obsidian pebble | Letlapana la obsidian | medium |
| `piece_quartz` | Quartz | Quartz | medium |
| `piece_quartz_full` | River-quartz pebble | Letlapana la quartz ya noka | medium |
| `piece_smoky` | Smoky | Ya musi | low |
| `piece_smoky_full` | Smoky rough diamond | Taemane e e sa segwang ya mmala wa musi | low |
| `piece_clear` | Clear | E e phepa | low |
| `piece_clear_full` | Clear rough diamond | Taemane e e sa segwang e e phepa | low |
| `egg_ostrich` | Ostrich | Ntšhe | high |
| `egg_ostrich_full` | Ostrich egg | Lee la ntšhe | high |
| `egg_guineafowl` | Guineafowl | Kgaka | high |
| `egg_guineafowl_full` | Helmeted guineafowl egg | Lee la kgaka | high |
| `egg_lapwing` | Lapwing | Lekgwaraganyane | low |
| `egg_lapwing_full` | Crowned lapwing egg | Lee la lekgwaraganyane | low |
| `egg_roller` | Roller | Tlhatlhagwe | low |
| `egg_roller_full` | Lilac-breasted roller egg | Lee la tlhatlhagwe | low |
| `egg_kori` | Kori | Kgori | high |
| `egg_kori_full` | Kori bustard egg | Lee la kgori | high |
| `scene_okavango` | Okavango Delta | Okavango Delta | high |
| `scene_okavango_short` | Okavango | Okavango | high |
| `scene_okavango_fact` | A UNESCO World Heritage Site since 2014, the List's 1,000th, and a great inland delta that never reaches the sea. | Lefelo la Boswa jwa Lefatshe la UNESCO go tloga ka 2014, la bo 1,000 mo lenaaneng, e bile ke delta e kgolo ya mo teng ga naga e e sa goroganeng kwa lewatleng. | medium |
| `scene_chobe` | Chobe National Park | Phaka ya Bosetšhaba ya Chobe | medium |
| `scene_chobe_short` | Chobe | Chobe | high |
| `scene_chobe_fact` | Botswana's first national park, where huge elephant herds gather on the Chobe River in the dry season. | Phaka ya bosetšhaba ya ntlha ya Botswana, kwa matlhape a magolo a ditlou a kopanelang gone mo nokeng ya Chobe mo letlhabula. | medium |
| `scene_makgadikgadi` | Makgadikgadi Pans | Makgadikgadi | high |
| `scene_makgadikgadi_short` | Makgadikgadi | Makgadikgadi | high |
| `scene_makgadikgadi_fact` | One of the largest salt pans on Earth: the dry bed of an ancient lake, ringed by ancient baobabs. | Nngwe ya dipane tsa letswai tse dikgolo go gaisa mo lefatsheng: bodilo jo bo omileng jwa letsha la bogologolo, bo dikologilwe ke mowana ya bogologolo. | medium |
| `scene_kalahari` | Central Kalahari | Kgalagadi e e Fa Gare | medium |
| `scene_kalahari_short` | Central Kalahari | Kgalagadi | medium |
| `scene_kalahari_fact` | Established in 1961, the Central Kalahari Game Reserve covers 52,800 km², larger than the Netherlands. | E thailwe ka 1961, Central Kalahari Game Reserve e bogolo jwa 52,800 km², e kgolo go feta Netherlands. | medium |
| `scene_tsodilo` | Tsodilo Hills | Dithaba tsa Tsodilo | high |
| `scene_tsodilo_short` | Tsodilo | Tsodilo | high |
| `scene_tsodilo_fact` | A UNESCO World Heritage Site with over 4,500 rock paintings in just 10 km², the "Louvre of the Desert". | Lefelo la Boswa jwa Lefatshe la UNESCO le le nang le ditshwantsho tsa mo mafikeng di feta 4,500 mo 10 km² fela, "Louvre ya Sekaka". | medium |
| `stats_title` | Stats | Dipalo | medium |
| `stats_subtitle` | Your results on this phone. Nothing is uploaded. | Dipoelo tsa gago mo founong e. Ga go sepe se se romelwang. | medium |
| `stats_empty` | No finished games yet. Play one and it shows up here. | Ga go ise go nne le motshameko o o fedileng. Tshameka mongwe mme o tla bonala fa. | medium |
| `stats_won` | Won | Fentse | medium |
| `stats_lost` | Lost | Fentswe | medium |
| `stats_drawn` | Drawn | Lekalekane | medium |
| `stats_row_computer` | Computer · %1$s | Khomputara · %1$s | high |
| `stats_row_spoken` | %1$s: %2$d won, %3$d lost, %4$d drawn | %1$s: o fentse %2$d, o fentswe %3$d, lo lekalekane %4$d | medium |
| `stats_streak` | Win streak %1$d · best %2$d | Diphenyo tse di latelanang %1$d · tse dintsi %2$d | low |
| `stats_pass_and_play_note` | In Pass & Play, won means Dark won and lost means Light won. | Mo go Fetisa o tshameke, fentse go raya gore Lefifi le fentse, fentswe go raya gore Lesweu le fentse. | low |
| `stats_reset` | Reset stats | Simolola dipalo sešwa | medium |
| `stats_reset_title` | Reset all stats? | Phimola dipalo tsotlhe? | medium |
| `stats_reset_body` | Every win, loss and draw count goes back to zero. This can't be undone. | Diphenyo tsotlhe, go fenngwa le go lekalekana di boela kwa go lefela. Seno ga se busetswe morago. | medium |
| `stats_reset_confirm` | Reset | Phimola | medium |
| `about_title` | About Mmele | Ka ga Mmele | high |
| `about_version` | Version %1$s (%2$d) | Kgatiso %1$s (%2$d) | medium |
| `about_privacy_terms` | Privacy and terms | Sephiri le melawana | medium |
| `about_privacy` | Privacy policy | Pholisi ya sephiri | medium |
| `about_terms` | Terms of use | Melawana ya tiriso | medium |
| `about_contact` | Contact | Ikgolaganye le rona | medium |
| `about_online_data` | Your online data | Tshedimosetso ya gago ya inthanete | medium |
| `about_online_off` | Online play isn't switched on in this version, so Mmele has no online data about you. Offline games never leave your phone. | Go tshameka mo inthaneteng ga go ise go bulwe mo kgatisong e, ka jalo Mmele ga e na tshedimosetso epe ya gago mo inthaneteng. Metshameko e e sa tlhokeng inthanete ga e tswe mo founong ya gago. | medium |
| `about_online_on` | Online play uses a random anonymous ID and stores your game rooms on Google Firebase. Finished rooms are deleted when the host leaves, and anything idle for 30 days is removed at the next monthly clean-up. You can delete everything now: | Go tshameka mo inthaneteng go dirisa nomoro e e sa itsiweng e e tlhophilweng ka tshoganetso, mme dikamore tsa gago di bolokwa mo Google Firebase. Dikamore tse di fedileng di phimolwa fa yo o di dirileng a tsamaya, mme sengwe le sengwe se se sa dirisiwang malatsi a le 30 se a tlosiwa mo phepafatsong e e latelang ya kgwedi. O ka phimola tsotlhe jaanong: | medium |
| `about_delete` | Delete my online data | Phimola tshedimosetso ya me ya inthanete | medium |
| `about_deleting` | Deleting… | E a phimolwa… | medium |
| `about_delete_title` | Delete your online data? | Phimola tshedimosetso ya gago ya inthanete? | medium |
| `about_delete_body` | This deletes every online room you created or joined from this phone, and your anonymous online ID. Games in progress will end. Offline play is not affected. | Seno se phimola dikamore tsotlhe tsa inthanete tse o di dirileng kgotsa o di tsenetseng ka founo e, le nomoro ya gago e e sa itsiweng. Metshameko e e tswelelang e tla fela. Go tshameka kwa ntle ga inthanete ga go amege. | medium |
| `about_delete_confirm` | Delete | Phimola | medium |
| `about_deleted_rooms (one)` | Done: %1$d room deleted. | Go weditswe: kamore e le %1$d e phimotswe. | medium |
| `about_deleted_rooms (other)` | Done: %1$d rooms deleted. | Go weditswe: dikamore di le %1$d di phimotswe. | medium |
| `about_id_deleted` | Your anonymous ID has been deleted. | Nomoro ya gago e e sa itsiweng e phimotswe. | medium |
| `about_id_pending` | Your anonymous ID was removed from this phone and will be erased from the server within 60 days. | Nomoro ya gago e e sa itsiweng e tlositswe mo founong e, e tla phimolwa mo seveng mo malatsing a le 60. | medium |
| `about_delete_failed` | Couldn't reach the server (%1$s). Check your connection and try again, or email %2$s. | Ga re a kgona go fitlhelela seva (%1$s). Tlhola kgolagano mme o leke gape, kgotsa o kwalele %2$s. | medium |
| `about_network_error` | network error | phoso ya kgolagano | medium |
| `about_credits` | Credits and sources | Ditebogo le metswedi | medium |
| `about_licences` | Open-source licences | Dilaesense tsa open-source | medium |
| `about_licences_intro` | Mmele is built with these open-source libraries. Tap one to read its licence. | Mmele e agilwe ka dilaeborari tse tsa open-source. Tobetsa nngwe go bala laesense ya yona. | medium |
| `about_licences_missing` | Licence list unavailable in this build. | Lenaane la dilaesense ga le yo mo kgatisong e. | medium |
| `about_read_licence` | read licence | bala laesense | medium |
| `about_see_website` | See website | Bona webosaete | medium |
| `about_website` | Website | Webosaete | medium |
| `credit_rules` | Morabaraba rules | Melao ya Morabaraba | high |
| `credit_eggs` | Bird eggs | Mae a dinonyane | high |
| `credit_art` | Art and sound | Botaki le modumo | medium |
| `credit_art_body` | Every board, scene, piece and sound is drawn or synthesised in code for Mmele. No third-party images, fonts or samples. | Boto nngwe le nngwe, lefelo, kgomo le modumo di tshwantshitswe kgotsa di dirilwe ka khoutu ya Mmele. Ga go na ditshwantsho, mekwalo kgotsa medumo ya batho ba bangwe. | medium |
| `credit_translation` | Setswana | Setswana | high |
| `credit_translation_body` | Setswana translation drafted for Mmele. Corrections are welcome at the contact address. | Phetolelo ya Setswana e kwaletswe Mmele. Diphoso di ka begwa kwa aterese ya kgolagano. | low |
| `game_hint` | Hint | Thuso | medium |
| `game_hint_thinking` | Thinking… | E a akanya… | medium |
| `game_hint_place` | Hint: place on %1$s | Thuso: baya mo %1$s | medium |
| `game_hint_move` | Hint: move %1$s to %2$s | Thuso: sutisa %1$s go ya %2$s | medium |
| `game_hint_shoot` | Hint: shoot the cow on %1$s | Thuso: thunya kgomo e e mo %1$s | medium |
| `game_replay` | Replay | Boeletsa | medium |
| `replay_title` | Replay | Boeletsa | medium |
| `replay_position` | Move %1$d of %2$d | Motsamao %1$d go %2$d | medium |
| `replay_start` | Start | Tshimologo | high |
| `replay_back` | Back one move | Motsamao o le mongwe morago | medium |
| `replay_forward` | Forward one move | Motsamao o le mongwe pele | medium |
| `replay_end` | End | Bokhutlo | high |
| `replay_action_place` | %1$s placed on %2$s | %1$s o beile mo %2$s | medium |
| `replay_action_move` | %1$s moved %2$s to %3$s | %1$s o sutisitse %2$s go ya %3$s | medium |
| `replay_action_shoot` | %1$s shot the cow on %2$s | %1$s o thuntse kgomo e e mo %2$s | medium |
| `replay_start_position` | Empty board | Boto e e lolea | medium |
| `illegal_game_over` | The game is over. | Motshameko o fedile. | high |
| `illegal_must_shoot` | You formed a mill: shoot one of the opponent's cows first. | O dirile mola: thunya nngwe ya dikgomo tsa mophenyisani pele. | medium |
| `illegal_shoot_enemy` | Shoot one of the opponent's cows. | Thunya nngwe ya dikgomo tsa mophenyisani. | medium |
| `illegal_protected` | That cow is protected in a mill. Shoot one that isn't. | Kgomo eo e sireletsegile mo moleng. Thunya e e seng mo moleng. | medium |
| `illegal_place_first` | Place all your cows before moving any. | Baya dikgomo tsotlhe pele o sutisa epe. | medium |
| `illegal_occupied` | That point is taken. Place on an empty point. | Ntlha eo e tšhwerwe. Baya mo ntlheng e e lolea. | medium |
| `illegal_not_your_cow` | Pick one of your own cows to move. | Tlhopha nngwe ya dikgomo tsa gago go e sutisa. | medium |
| `illegal_destination_occupied` | That point is taken. Move to an empty point. | Ntlha eo e tšhwerwe. Sutisetsa kwa ntlheng e e lolea. | medium |
| `illegal_slide_only` | That cow can only slide along a line to a connected empty point. | Kgomo eo e ka sutela fela mo moleng go ya kwa ntlheng e e lolea e e gaufi. | medium |
| `illegal_step_or_jump` | Step to a connected empty point, or jump over the middle cow into the empty end. | Gata go ya kwa ntlheng e e lolea e e gaufi, kgotsa tlolela godimo ga kgomo e e fa gare go ya kwa bokhutlong jo bo lolea. | medium |
| `illegal_stuck` | That cow is boxed in. Try another one. | Kgomo eo e tswaletswe. Leka e nngwe. | medium |
| `illegal_remill` | You can't move straight back to remake the mill you just broke. | Ga o kgone go boela kwa morago ka bonako go dira gape mola o o sa tswang go o thuba. | low |
| `home_moruba` | Moruba | Moruba | high |
| `home_moruba_sub` | Four-row mancala · vs Computer or Pass & Play | Moruba wa mela e mene · le khomputara kgotsa Fetisa o tshameke | medium |
| `moruba_title` | Moruba | Moruba | high |
| `moruba_rules_short` | Sow counter-clockwise round your own two rows, one stone per pit. Land in a full pit to pick it up and keep sowing; land in an empty inner pit to capture the two enemy pits facing it. Leave your opponent without a move to win. | Jala o dikologa kgatlhanong le tsamaiso ya oura mo melelong ya gago e mebedi, leje le le lengwe mo khuting nngwe le nngwe. Fa o wela mo khuting e e nang le majana, a tsee o tswelele o jala; fa o wela mo khuting e e se nang sepe ya mo teng, o tsaya dikhuti tse pedi tsa mophenyisani tse di lebaganeng le yone. Tlogela mophenyisani a sena motsamao gore o fenye. | low |
| `moruba_opponent` | Opponent | Mophenyisani | high |
| `moruba_holes` | Holes per row | Dikhuti mo moleng | medium |
| `moruba_stones` | Stones per pit | Majana mo khuting | medium |
| `moruba_computer` | Computer | Khomputara | high |
| `moruba_you` | You | Wena | high |
| `moruba_south` | South | Borwa | high |
| `moruba_north` | North | Bokone | high |
| `moruba_stones_on_board` | %1$d stones | Majana a le %1$d | medium |
| `moruba_sowing` | Sowing… %1$d in hand | Go jala… %1$d mo seatleng | low |
| `moruba_thinking` | Computer is thinking… | Khomputara e a akanya… | high |
| `moruba_your_turn` | Your turn: tap a pit with 2 or more stones | Ke nako ya gago: tobetsa khuti e e nang le majana a le 2 kgotsa go feta | medium |
| `moruba_side_turn` | %1$s to play | Ke nako ya %1$s | medium |
| `moruba_board_desc` | Moruba board: four rows of pits; yours are the bottom two rows | Boto ya Moruba: mela e mene ya dikhuti; tsa gago ke mela e mebedi ya kwa tlase | low |
| `fact_did_you_know` | Did you know? | A o ne o itse? | high |
| `about_photo_credits` | Photo credits | Ba ba tsereng ditshwantsho | low |
| `about_photo_credits_body` | Photos from Wikimedia Commons, used under the licences shown (CC0, public domain, CC BY, CC BY-SA). Tap a photo to open its source page. | Ditshwantsho di tswa kwa Wikimedia Commons, di dirisiwa ka dilaesense tse di bontshitsweng (CC0, public domain, CC BY, CC BY-SA). Tobetsa setshwantsho go bula tsebe ya sone. | medium |
| `chat_title` | Chat | Puisano | medium |
| `chat_mute` | Mute chat | Tidimatsa puisano | medium |
| `chat_type_hint` | Type a message | Kwala molaetsa | medium |
| `chat_remaining` | %1$d left | %1$d tse di setseng | low |
| `chat_send` | Send | Romela | high |
| `chat_rules_note` | Be kind. Bad words are hidden. Long-press a message to report or block. | Nna pelonomi. Mafoko a a maswe a a fitlhwa. Tobetsa molaetsa sebaka go o bega kgotsa go thibela. | low |
| `chat_report` | Report | Bega | medium |
| `chat_block` | Block player | Thibela motshameki | medium |
| `chat_reported` | Report sent. Thank you. | Pego e romilwe. Re a leboga. | medium |
| `chat_blocked` | Player blocked. You won't see their messages. | Motshameki o thibetswe. Ga o kitla o bona melaetsa ya gagwe. | low |
| `chat_too_fast` | Slow down a little. | Iketle go se kae. | medium |
| `chat_too_long` | That message is too long. | Molaetsa oo o moleele thata. | medium |
| `chat_failed` | Message not sent. | Molaetsa ga o a romelwa. | medium |
| `settings_chat` | In-game chat | Puisano mo motshamekong | medium |
| `settings_analytics` | Share usage & crash data | Abelana tiriso le diphoso | low |
| `ranked_toggle` | Ranked match (virtual points) | Motshameko wa maemo (dintlha tsa maitirelo) | low |
| `points_disclaimer_short` | Points have no cash value. | Dintlha ga di na boleng jwa madi. | low |
| `points_disclaimer` | Points are virtual only: they have no cash value and cannot be bought, sold or cashed out. | Dintlha ke tsa maitirelo fela: ga di na boleng jwa madi, ga di rekwe, ga di rekisiwe e bile ga di fetolelwe madi. | low |
| `leaderboard_title` | Leaderboard | Lenaane la baeteledipele | low |
| `ranked_offline` | Online play is not set up in this build. | Motshameko wa inthanete ga o a baakanngwa mo kgatisong e. | low |
| `ranked_my_line` | Your rating %1$d · %2$d points · %3$d ranked games | Maemo a gago %1$d · dintlha %2$d · metshameko ya maemo %3$d | low |
| `ranked_how` | Each ranked match stakes %1$d points; the winner takes them. Results are settled by the server. | Motshameko mongwe le mongwe wa maemo o beela dintlha %1$d; mofenyi o a di tsaya. Dipholo di swetswa ke sefatlhego sa inthanete. | low |
| `leaderboard_empty` | No ranked games yet. | Ga go ise go nne metshameko ya maemo. | medium |
| `ranked_points_short` | %1$d pts | dintlha %1$d | low |
| `tournaments_title` | Tournaments | Dikgaisano | medium |
| `tournaments_create` | New tournament | Kgaisano e ntšha | medium |
| `tournaments_create_button` | Create | Tlhama | medium |
| `tournaments_name` | Name | Leina | high |
| `tournaments_players` | %1$d players | Batshameki %1$d | medium |
| `tournaments_entry_line` | Entry %1$d points · 1st %2$d · 2nd %3$d | Go tsena dintlha %1$d · wa ntlha %2$d · wa bobedi %3$d | low |
| `tournaments_empty` | No tournaments yet. Create one! | Ga go ise go nne dikgaisano. Tlhama e le nngwe! | medium |
| `tournaments_open` | open | e butswe | medium |
| `tournaments_running` | playing | go a tshamekwa | medium |
| `tournaments_finished` | finished | e fedile | medium |
| `tournaments_rejected` | Entry refused: %1$s | Go tsena go ganetswe: %1$s | low |
| `tournaments_join` | Join (%1$d points) | Tsena (dintlha %1$d) | medium |
| `tournaments_leave` | Leave (refund) | Tswa (busediwa) | medium |
| `tournaments_round` | Round %1$d | Kgaolo %1$d | medium |
| `tournaments_play` | Play | Tshameka | high |
| `tournaments_standings` | Standings | Maemo | medium |
| `academy_title` | Academy | Sekolo | medium |
| `academy_subtitle` | Five short puzzles that teach the ideas behind Mmele. | Dithaelo tse tlhano tse dikhutshwane tse di rutang dikakanyo tsa Mmele. | medium |
| `academy_progress` | %1$d of %2$d lessons done | Dithuto di le %1$d go %2$d di weditswe | medium |
| `academy_locked` | Finish the lesson before to unlock | Fetsa thuto e e fetileng go e bula | medium |
| `academy_done` | Done | E weditswe | medium |
| `academy_start` | Start | Simolola | high |
| `academy_again` | Play again | Tshameka gape | high |
| `academy_lesson_number` | Lesson %1$d | Thuto %1$d | high |
| `lesson_goal_label` | Goal | Maikaelelo | medium |
| `lesson_hint` | Hint | Thuso | medium |
| `lesson_next_hint` | Another hint | Thuso e nngwe | medium |
| `lesson_restart` | Restart | Simolola sešwa | medium |
| `lesson_begin` | Begin | Simolola | high |
| `lesson_wrong` | Not quite. That lets Light off the hook. Try again from the same position. | Nnyaa sentle. Seo se golola Lesweu. Leka gape go tswa mo maemong a a tshwanang. | low |
| `lesson_wrong_punished` | Not quite. Watch Light's answer, then try again. | Nnyaa sentle. Leba karabo ya Lesweu, mme o leke gape. | medium |
| `lesson_good` | Good move. Light answers… your turn again. | Motsamao o montle. Lesweu le a araba… ke nako ya gago gape. | medium |
| `lesson_shoot_now` | Mill! Now tap a Light cow to shoot it. | Mola! Jaanong tobetsa kgomo e tshweu go e thunya. | medium |
| `lesson_your_turn` | Your move. You play Dark. | Ke nako ya gago. O tshameka ka Lefifi. | medium |
| `lesson_solved_title` | Well done! | O dirile sentle! | high |
| `lesson_next` | Next lesson | Thuto e e latelang | high |
| `lesson_back_to_academy` | Back to the Academy | Boela kwa Sekolong | medium |
| `lesson_practice` | Practice | Itlwaetso | medium |
| `lesson_practice_intro` | A fresh position, the same idea. No hints this time. | Maemo a masha, kakanyo e e tshwanang. Ga go na thuso lekgetlho leno. | medium |
| `lesson_practice_cta` | Now try it yourself | Jaanong leka ka bowena | medium |
| `lesson_practice_done` | Practice done. The next lesson is unlocked. | Itlwaetso e weditswe. Thuto e e latelang e butswe. | medium |
| `lesson_all_done` | You have finished the Academy. Go and beat the computer! | O feditse Sekolo. Jaanong ya o fenye khomputara! | medium |
| `lesson_mill_title` | Making a mill | Go dira mola | medium |
| `lesson_mill_intro` | Three of your cows in a straight line along a drawn line is a mill. Every time you complete a mill you shoot (remove) one of your opponent's cows. You have two cows on the top row already. | Fa dikgomo tsa gago tse tharo di le mo moleng o o tlhamaletseng o o tshwantshitsweng, ke mola. Nako nngwe le nngwe fa o feleletsa mola o thunya (o tlosa) nngwe ya dikgomo tsa mophenyisani. O setse o na le dikgomo di le pedi mo moleng wa kwa godimo. | medium |
| `lesson_mill_goal` | Complete a mill, then shoot one of Light's cows. | Feleletsa mola, mme o thunye nngwe ya dikgomo tsa Lesweu. | medium |
| `lesson_mill_hint1` | Look along the top edge of the board. | Leba fa thoko ga boto kwa godimo. | medium |
| `lesson_mill_hint2` | Place a cow on the top-right corner, then tap any light cow. | Baya kgomo mo sekhutlong sa kwa godimo ka fa mojeng, mme o tobetse kgomo epe e tshweu. | medium |
| `lesson_mill_success` | That is a mill: three in a line, and Light is one cow down. Watch for your opponent's two-in-a-rows too, and block them. | Oo ke mola: tse tharo mo moleng, mme Lesweu le latlhegetswe ke kgomo. Ela tlhoko gape di le pedi tsa mophenyisani mo moleng, o di thibe. | medium |
| `lesson_trap_title` | Trapping vs shooting | Go tswala kgatlhanong le go thunya | medium |
| `lesson_trap_intro` | Shooting is not the only way to win. If your opponent cannot move any cow on their turn, they are locked in (go tswala) and lose at once. Light's four cows in the top-left corner have just one free point left. | Go thunya ga se tsela fela ya go fenya. Fa mophenyisani a sa kgone go sutisa kgomo epe mo nakong ya gagwe, o tswaletswe (go tswala) mme o a fenngwa ka bonako. Dikgomo tse nne tsa Lesweu mo sekhutlong sa kwa godimo ka fa molemeng di na le ntlha e le nngwe fela e e lolea. | medium |
| `lesson_trap_goal` | Win in one move by leaving Light with no legal move. | Fenya ka motsamao o le mongwe ka go tlogela Lesweu go se na motsamao o o letleletsweng. | medium |
| `lesson_trap_hint1` | Find the one empty point next to a light cow. | Batla ntlha e le nngwe e e lolea e e gaufi le kgomo e tshweu. | medium |
| `lesson_trap_hint2` | It sits on the diagonal from the top-left corner. Step onto it with the cow from the inner square. | E mo moleng o o kgabaganyang go tswa kwa sekhutlong sa kwa godimo ka fa molemeng. Gatela mo go yona ka kgomo e e mo sekwereng sa ka fa teng. | low |
| `lesson_trap_success` | Locked in! No shot needed. Against a crowded board, trapping can be faster than shooting. | O mo tswaletse! Ga go tlhokege go thunya. Fa boto e tletse, go tswala go ka nna bonako go feta go thunya. | medium |
| `lesson_escape_title` | Escaping a lock | Go tswa mo go tswaleleng | medium |
| `lesson_escape_intro` | Now you are the one being squeezed. Light's cows surround yours, and most moves let Light shut the last door. A mill lets you shoot a cow out of the wall. | Jaanong ke wena o o pitlaganngwang. Dikgomo tsa Lesweu di dikologile tsa gago, mme metsamao e mentsi e letla Lesweu go tswala kgoro ya bofelo. Mola o go letla go thunya kgomo mo loboteng. | low |
| `lesson_escape_goal` | Keep at least one legal move through Light's next two turns. | Nna o na le motsamao o le mongwe bonnye o o letleletsweng mo dinakong tse pedi tse di latelang tsa Lesweu. | low |
| `lesson_escape_hint1` | Do not just shuffle: form a mill so you can shoot a blocker. | O seka wa sutisa fela: dira mola gore o kgone go thunya e e go thibang. | medium |
| `lesson_escape_hint2` | Look at the middle line coming down from the top edge. After the mill any shot will do; the cow on the right edge frees you the most. | Leba mola wa fa gare o o tswang kwa godimo. Morago ga mola thunyo epe e siame; kgomo e e fa thoko ka fa mojeng e go golola thata. | low |
| `lesson_escape_success` | You broke out. When you are short of moves, a mill that opens space is worth more than any other cow. | O tswile. Fa o tlhaela metsamao, mola o o bulang sebaka o botlhokwa go feta kgomo epe e nngwe. | medium |
| `lesson_fly_title` | Flying tactics | Maano a go fofa | medium |
| `lesson_fly_intro` | With only three cows left you may fly (go fofa): move a cow to any empty point. Light is flying too and threatens to complete a line on the right side next turn. | Fa o setse ka dikgomo tse tharo fela o ka fofa (go fofa): sutisetsa kgomo kwa ntlheng epe e e lolea. Lesweu le lone le a fofa mme le tshosetsa go feleletsa mola ka fa mojeng mo nakong e e latelang. | medium |
| `lesson_fly_goal` | Win this turn: Light also has three cows, so one shot is enough. | Fenya mo nakong e: Lesweu le lone le na le dikgomo tse tharo, ka jalo thunyo e le nngwe e lekane. | medium |
| `lesson_fly_hint1` | Blocking Light is fine, but striking first is better. | Go thibela Lesweu go siame, mme go itaya pele go botoka. | medium |
| `lesson_fly_hint2` | Your two cows on the bottom row need a third: fly to the bottom-right corner. | Dikgomo tsa gago tse pedi mo moleng wa kwa tlase di tlhoka ya boraro: fofela kwa sekhutlong sa kwa tlase ka fa mojeng. | medium |
| `lesson_fly_success` | When both sides fly, the first mill usually decides. Always check for a winning flight before you defend. | Fa ka bobedi ba fofa, mola wa ntlha o tlwaetse go swetsa. Ka metlha tlhola fa go na le phofo e e fenyang pele o itshireletsa. | low |
| `lesson_tri_title` | Khutlotharo | Khutlotharo | high |
| `lesson_tri_intro` | Khutlotharo is the triangle game: three cows each, and the first line of three wins outright. A cow may also jump over a neighbouring cow to the empty point straight beyond it. | Khutlotharo ke motshameko wa khutlotharo: dikgomo tse tharo mongwe le mongwe, mme mola wa ntlha wa tse tharo o a fenya. Kgomo e ka tlolela gape godimo ga kgomo e e gaufi go ya kwa ntlheng e e lolea e e fa morago ga yona. | medium |
| `lesson_tri_goal` | Win now. Light also threatens the top point. | Fenya jaanong. Lesweu le lone le tshosetsa ntlha ya kwa godimo. | medium |
| `lesson_tri_hint1` | The empty point at the top completes your centre line. | Ntlha e e lolea kwa godimo e feleletsa mola wa gago wa fa gare. | medium |
| `lesson_tri_hint2` | Your bottom-left cow can jump over the light cow above it to reach the top. | Kgomo ya gago ya kwa tlase ka fa molemeng e ka tlolela godimo ga kgomo e tshweu e e fa godimo ga yona go fitlha kwa godimo. | medium |
| `lesson_tri_success` | A jump wins it. In Khutlotharo, always count jumps as well as steps, for both sides. | Go tlola go a fenya. Mo Khutlotharo, ka metlha bala ditlolo le metsamao, ya batshameki ka bobedi. | medium |
| `howto_title` | How to play | Ka moo go tshamekwang ka teng | high |
| `howto_offer` | New to Morabaraba? Read the rules in a minute. | O mosha mo Morabarabeng? Bala melao mo motsotsong o le mongwe. | medium |
| `howto_show` | Show me | Mpontshe | high |
| `howto_skip` | Not now | E seng jaanong | high |
| `howto_done` | Got it | Ke tlhalogantse | high |
| `howto_next` | Next | E e latelang | high |
| `howto_page` | %1$d / %2$d | %1$d / %2$d | high |
| `howto_place_title` | 1 · Go baya: place | 1 · Go baya | high |
| `howto_place_body` | Each player has 12 cows. Take turns placing them on any empty point, Dark first. | Motshameki mongwe le mongwe o na le dikgomo di le 12. Lo baya ka go refosana mo ntlheng epe e e lolea, Lefifi le simolola. | medium |
| `howto_mill_title` | 2 · Make a mill, shoot | 2 · Dira mola, thunya | medium |
| `howto_mill_body` | Three of your cows in a straight line is a mill. Each new mill lets you shoot one opposing cow. Cows in a mill are safe while the opponent has others to shoot. | Dikgomo tsa gago tse tharo mo moleng o o tlhamaletseng ke mola. Mola mongwe le mongwe o mosha o go letla go thunya kgomo e le nngwe ya mophenyisani. Dikgomo tse di mo moleng di sireletsegile fa mophenyisani a na le tse dingwe tse a ka di thunyang. | medium |
| `howto_move_title` | 3 · Go sutisa: move | 3 · Go sutisa | high |
| `howto_move_body` | When all cows are placed, move one cow per turn along a line to a neighbouring empty point. Break a mill and remake it to shoot again. | Fa dikgomo tsotlhe di beilwe, sutisa kgomo e le nngwe ka nako mo moleng go ya kwa ntlheng e e lolea e e gaufi. Thuba mola mme o o dire gape go thunya gape. | medium |
| `howto_fly_title` | 4 · Go fofa: fly | 4 · Go fofa | high |
| `howto_fly_body` | Down to three cows? Your cows may fly to any empty point. | O setse ka dikgomo tse tharo? Dikgomo tsa gago di ka fofela kwa ntlheng epe e e lolea. | medium |
| `howto_win_title` | 5 · Winning | 5 · Go fenya | high |
| `howto_win_body` | You win when your opponent has only two cows left or cannot move on their turn. Try the Academy for guided puzzles. | O fenya fa mophenyisani a setse ka dikgomo di le pedi fela kgotsa a sa kgone go sutisa mo nakong ya gagwe. Leka Sekolo go bona dithaelo tse di kaelwang. | medium |
