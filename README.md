# Emoji Chat Helper

Type emoji in Old School RuneScape chat the way you do in Slack and Discord. Start typing `:swe` and a list shows the emoji that match, what is left to type of each, and the short emoticons RuneLite already knows, like `Xd` and `<3`.

![The list of matches while typing :swe](docs/screenshots/preview-search.png)

## What it does

- **A list of matches while you type.** Start a name after a colon, like `:swe`, and the emoji that match are listed just above the chatbox, best first. What you have typed is grey and what is left to type is yellow.
- **Opens on the first colon.** A colon on its own lists RuneLite's emoticons that start with one, like `:)`, `:D` and `:P`.
- **Emoticons too.** Typing an emoticon such as `Xd` or `<3` lists the emoji it becomes. Where a name has one, it is shown beside it in green.
- **Finds what you mean.** Names are found by their start, by a word in them, or with a letter wrong, missing, extra or swapped: `:simle` finds `:smile:`.
- **Slack's names find RuneLite's.** `:sweat_smile:` finds 😅 and tells you RuneLite's name for it, `:grinning_face_with_sweat:`, which other players see as the emoji too.
- **Emoji in chat.** A `:name:` in public, friends, clan and private chat, and over players' heads, shows as the emoji.

The list only shows names. RuneLite does not allow plugins to type into the chatbox, so you type the rest yourself.

| Typing a colon | Typing an emoticon | Only emoticons |
|---|---|---|
| ![A lone colon lists the emoticons that start with one](docs/screenshots/preview-colon.png) | ![Xd lists the emoji it becomes](docs/screenshots/preview-emoticon.png) | ![With Only emoticons on, :smil lists emoticons](docs/screenshots/preview-emoticons-only.png) |

## Who sees the emoji

What you send is the text you type. How others see it depends on their plugins. By default this plugin only shows emoji for what other players see too: RuneLite's names and emoticons, which RuneLite's own Emojis plugin shows to anyone who has it on.

| Sent | With this plugin | With RuneLite's Emojis plugin | With neither |
|---|---|---|---|
| `:grinning_face_with_sweat:` | 😅 | 😅 | the text |
| `Xd` | 😆, with RuneLite's Emojis plugin on too | 😆 | the text |
| `:sweat_smile:` | 😅, with **Only names others see** off | the text | the text |

RuneLite reads names and emoticons only in their own case: `Xd` works, `XD` and `xd` do not.

## Settings

| Setting | Does |
|---|---|
| List matches while typing | Turns the list on or off |
| List position | Above the chatbox, just above the line you type in, or anywhere: type a colon so the list shows, then hold Alt and drag it |
| Matches listed | How many matches the list shows, 1 to 10 |
| Show other name | Shows the Slack name that found a match, or RuneLite's name for a Slack name |
| Emoji in chat | Shows `:names:` in chat and over heads as emoji |
| Only names others see | Shows and lists only RuneLite's names, which players with its Emojis plugin see too. On by default |
| Only emoticons | Lists only emoji with a RuneLite emoticon, like `Xd` or `<3`, with the emoticon to type |

The **Style** section sets how the list looks: its font and font size, emoji size, padding, row spacing, text shadow, background (its alpha sets how see-through it is), border, and each colour.

## Credits

Emoji pictures are [Twemoji](https://github.com/jdecked/twemoji), as RuneLite ships them. Slack's names come from [iamcal/emoji-data](https://github.com/iamcal/emoji-data). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
