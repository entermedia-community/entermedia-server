package org.entermediadb.videoedit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.openedit.page.Page;

import com.coremedia.iso.IsoBufferWrapperImpl;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoOutputStream;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

public class VideoEditor
{
		public void split(Page inVideo, double inStartTime, double cutto, OutputStream inOutput) throws IOException
		{
			File videofile = new File(inVideo.getContentItem().getAbsolutePath());
			Movie movie = new MovieCreator().build(new IsoBufferWrapperImpl(videofile));

	        List<Track> tracks = movie.getTracks();
	        movie.setTracks(new LinkedList<Track>());
	        
	        boolean timeCorrected = false;

	        // Here we try to find a track that has sync samples. Since we can only start decoding
	        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
	        // such a frame
	        for (Track track : tracks) {
	            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
	                if (timeCorrected) {
	                    // This exception here could be a false positive in case we have multiple tracks
	                    // with sync samples at exactly the same positions. E.g. a single movie containing
	                    // multiple qualities of the same video (Microsoft Smooth Streaming file)

	                    throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
	                }
	                inStartTime = correctTimeToNextSyncSample(track, inStartTime);
	                timeCorrected = true;
	            }
	        }
	        
	        for (Track track : tracks) {
	            long currentSample = 0;
	            double currentTime = 0;
	            long startSample = -1;
	            long endSample = -1;

	            for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
	                TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
	                //find the first sample
	                for (int j = 0; j < entry.getCount(); j++) {
	                    // entry.getDelta() is the amount of time the current sample covers.

	                    if (currentTime <= inStartTime) {
	                        // current sample is still before the new starttime
	                        startSample = currentSample;
	                    } else {
	                        // current sample is after the end of the cropped video
	                        break;
	                    }
	                    currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
	                    currentSample++;
	                }
	            }
	            //make the end sample the last sample
	            endSample = track.getSamples().size();
	            movie.addTrack(new CroppedTrack(track, startSample, endSample));
	        }
	        
	        IsoFile out = new DefaultMp4Builder().build(movie);
	        IsoOutputStream stream = new IsoOutputStream(inOutput);
	        out.getBox(stream);
	        stream.flush();
		}

	    protected static double correctTimeToNextSyncSample(Track track, double cutHere) {
	        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
	        long currentSample = 0;
	        double currentTime = 0;
	        for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
	            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
	            for (int j = 0; j < entry.getCount(); j++) {
	                if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
	                    // samples always start with 1 but we start with zero therefore +1
	                    timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
	                }
	                currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
	                currentSample++;
	            }
	        }
	        for (double timeOfSyncSample : timeOfSyncSamples) {
	            if (timeOfSyncSample > cutHere) {
	                return timeOfSyncSample;
	            }
	        }
	        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
	    }


}
