export default AudioTrackList;
/**
 * The current list of {@link AudioTrack} for a media file.
 *
 * @see [Spec]{@link https://html.spec.whatwg.org/multipage/embedded-content.html#audiotracklist}
 * @extends TrackList
 */
declare class AudioTrackList extends TrackList {
    /**
     * Create an instance of this class.
     *
     * @param {AudioTrack[]} [tracks=[]]
     *        A list of `AudioTrack` to instantiate the list with.
     */
    constructor(tracks?: AudioTrack[]);
    changing_: boolean;
    /**
     * Add an {@link AudioTrack} to the `AudioTrackList`.
     *
     * @param {AudioTrack} track
     *        The AudioTrack to add to the list
     *
     * @fires TrackList#addtrack
     */
    addTrack(track: AudioTrack): void;
    removeTrack(rtrack: any): void;
}
import TrackList from './track-list';
import type AudioTrack from './audio-track';
//# sourceMappingURL=audio-track-list.d.ts.map