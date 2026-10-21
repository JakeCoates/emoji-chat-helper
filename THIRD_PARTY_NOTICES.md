# Third-party notices

## Twemoji graphics

`src/main/resources/com/emojichathelper/emoji.png` holds the 13x13 emoji pictures that RuneLite serves to its Emojis plugin (`https://static.runelite.net/emoji/assets.zip`), arranged into one sheet. They are Twemoji graphics:

Copyright 2019 Twitter, Inc and other contributors. Graphics licensed under CC-BY 4.0: https://creativecommons.org/licenses/by/4.0/

Changes: RuneLite scaled them down to 13x13; this plugin arranges them into a sheet.

## RuneLite emoji names

The first two columns of `src/main/resources/com/emojichathelper/emoji.txt`, each emoji's codepoint and name, come from RuneLite's emoji index (`https://static.runelite.net/emoji/index.json`), so this plugin and RuneLite's Emojis plugin agree on what a name means.

## RuneLite emoticons

`src/main/resources/com/emojichathelper/emoticons.txt` lists the emoticons RuneLite's Emojis plugin turns into emoji (`:)`, `Xd`, `<3`), taken from the RuneLite client's `Emoji.java` (https://github.com/runelite/runelite, BSD 2-Clause License; Copyright (c) 2019, Lotto; Copyright (c) 2025, Adam), so the list can show them.

## emoji-data

The Slack names in `src/main/resources/com/emojichathelper/emoji.txt` come from emoji-data (https://github.com/iamcal/emoji-data):

The MIT License (MIT)

Copyright (c) 2013 Cal Henderson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
