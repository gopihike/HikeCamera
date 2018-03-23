package wiki.hike.neo.hikecamera.encoder;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

/**
 * Created by gauravgoyal on 07/09/17.
 */

public class SoundFilter extends MediaPlayer {

    public MediaPlayer mp;

    public SoundFilter() {
        mp  = new MediaPlayer();
    }

    public void setFilter(Context context, String filePath) {
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mp.setDataSource(context, Uri.fromFile(new File(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepareAndStartFilter() {
        mp.prepareAsync();
        mp.setLooping(true);
        mp.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
    }

    public void startFilterSound() {
        if(mp != null) {
            mp.start();
        }
    }
    public void pauseFilterSound() {
        if(mp != null) mp.pause();
    }

    public void stopFilterSound() {
        if(mp!=null) mp.stop();
    }

    public void resetFilterSound() {
        if(mp != null) mp.reset();
    }

    public void releaseFilter() {
        if(mp != null) {
            mp.release();
            mp = null;
        }
    }

    public void resumeFilterSound() {
        if(mp != null){
            mp.seekTo(-1);
            mp.start();
        }
    }

    public void seekToTime(int time){
        if(mp != null){
            mp.seekTo(time);
        }
    }
}
