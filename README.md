# Subtick

Subtick is a carpet extension which lets you step through piston block events in the middle of a tick one by one. It ensures that all tick phases and dimensions are kept in order.

## Installation

Download the jar file from the releases tab and add to your mods folder, along with fabric carpet

## Commands
* be step: steps a single block event
* bed step: steps through all block events in the current block event delay
* be play: plays through block events on a given time interval
* bed play: plays through entire block event delays on a given time interval
* be count: says the number of block events currently in the queue
* now: says which tick phase the game is currently in

## Carpet Rules
* includeInvalidBlockEvents: step through block events executed on the wrong block type (these will never affect anything)
* vanillaPistonAnimations: let moving block animations play to completion on carpet clients (only really works on servers)
* highlightBlockEvents: show where block events take place to clients using glowing highlights
* losslessBedrockBreaking: changes one line of code that makes lossless bedrock breaking possible again (not vanilla lol, false by default)

## License
[MIT](https://choosealicense.com/licenses/mit/)
