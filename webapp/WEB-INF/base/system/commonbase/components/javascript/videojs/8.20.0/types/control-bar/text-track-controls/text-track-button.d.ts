export default TextTrackButton;
/** @import Player from '../../player' */
/**
 * The base class for buttons that toggle specific text track types (e.g. subtitles)
 *
 * @extends MenuButton
 */
declare class TextTrackButton {
    /**
     * Creates an instance of this class.
     *
     * @param {Player} player
     *        The `Player` that this class should be attached to.
     *
     * @param {Object} [options={}]
     *        The key/value store of player options.
     */
    constructor(player: Player, options?: any);
    /**
     * Create a menu item for each text track
     *
     * @param {TextTrackMenuItem[]} [items=[]]
     *        Existing array of items to use during creation
     *
     * @return {TextTrackMenuItem[]}
     *         Array of menu items that were created
     */
    createItems(items?: TextTrackMenuItem[], TrackMenuItem?: typeof TextTrackMenuItem): TextTrackMenuItem[];
    kinds_: any[];
}
import TextTrackMenuItem from './text-track-menu-item.js';
import type Player from '../../player';
//# sourceMappingURL=text-track-button.d.ts.map