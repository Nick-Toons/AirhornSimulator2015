# Dev Journal

## 2/14/2015
I have added the SoundPlayer class, whos job is to load sound files from the assets folder, be able to list them, and to play sounds. I will describe the API for it here:
|-----------------------------|----------------------------------------------------------------|
| Signature                   | Description                                                    |
|-----------------------------|----------------------------------------------------------------|
| Sound                       | The sound data class which stores information about each sound |
| void loadSounds()           | Loads all the sounds in the sound directory                    |
| List<Sound> getSounds()     | List the sounds that were loaded                               |
| void playSound(Sound sound) | Plays a sound using a SoundPool                                |
|-----------------------------|----------------------------------------------------------------|

