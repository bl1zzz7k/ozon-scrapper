package ru.ozon.api;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

public class BeepTest {
    @SneakyThrows
    @Test
    void test() {

        try (InputStream file = getClass().getClassLoader().getResourceAsStream("complete.wav");
             AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
             Clip clip = AudioSystem.getClip()) {
            // load the sound into memory (a Clip)
            clip.open(audioStream);
//            clip.setFramePosition(0);  // Must always rewind!
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(5.0f); // Reduce volume by 10 decibels.
            clip.start();
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.drain();
            Thread.sleep(clip.getMicrosecondLength() / 1000);
            Thread.sleep(10000);
            clip.stop();
        }
    }
}
