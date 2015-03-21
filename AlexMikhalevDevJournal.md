# Dev Journal

## 2/14/2015
I have added the SoundPlayer class, whos job is to load sound files from the 
assets folder, be able to list them, and to play sounds. I will describe the API 
for it here:

| Signature                   | Description                                                    |
|-----------------------------|----------------------------------------------------------------|
| Sound                       | The sound data class which stores information about each sound |
| void loadSounds()           | Loads all the sounds in the sound directory                    |
| List<Sound> getSounds()     | List the sounds that were loaded                               |
| void playSound(Sound sound) | Plays a sound using a SoundPool                                |

## 2/20/2015
I have spent some time fixing the SoundPlayer class. There were some issues with 
it not loading the sounds properly because of how Android asset paths work. It 
correctly loads the airhorn.wav sound now. There is a working api for listing 
and playing sounds that is described above. The next step is to implement a UI 
with a list of buttons which is created from the results of the `loadSounds()` 
method and plays the sounds using `playSound()`. I have also added a toString()
implementation for SoundPlayer.Sound for enhanced debugging. It should be 
possible to have basic app functionality soon. Something that may be added in 
the future is icons for sounds, which might need a change in implementation, 
but the flexibility of using this abstraction makes it relativly easy to add 
additional properties to sounds. Another thing that needs to be considered is 
that assets can not be dynamically added at runtime because they are not 
actually stored on the filesystem. They are compressed as part of the .apk 
file. So if we want to be able to implement the store functionality we will 
have to change the implementation of sound storage.

## 2/27/2015
I have spent some more time on the app. The wow sound effect that nick added didn't work with the
SoundPool for some reason. It would give an error like "could not load file (null)" and not play.
To fix this I converted the sound to a mp3 file using the following ffmpeg command

```sh
ffmpeg -i wow.wav -coded:a libmp3flac wow.mp3
```

I also fixed a bug where it would try to load some `bootanim.raw` file. I have no idea what this
file is and why it would try to load it, but it is fixed now. There is now a basic ui for playing
sounds. There is a single GridView in the main activity that has a list of all the sounds in the
assets/sounds folder. It isn't very polished yet because it just shows the result of toString() on
the Sound class. In the future this should be changed to use a custom ListAdapter, which can show
more things like an icon. But for now this works. You can successfully annoy schreiber with two
whole sounds.

## 3/20/2015
I have added the sound processing library necessary for the pitch shifting audio playing to the
project. It is called TarsosDSP and it is open source and free. It has an example called Catify
which does something similar to what we need in that it takes a series of notes and an audio
sample and makes a song using those. All I need to do is adapt that code for android, which may be
difficult to do because the audio stack is completely different. They do provide some classes for
interfacing with android audio but I am not yet sure if they are sufficient. So far, this is the
only FOSS and pure java audio processing library that I could find. Any others either have native
components or are not free, which makes them more difficult to use. I am hoping to get a working
example by next week, but it did take a bit of work to get this library to be recognized by android
studio and gradle.