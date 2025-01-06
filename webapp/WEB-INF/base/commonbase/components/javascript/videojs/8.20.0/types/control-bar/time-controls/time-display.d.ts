export default TimeDisplay;
/** @import Player from '../../player' */
/**
 * Displays time information about the video
 *
 * @extends Component
 */
declare class TimeDisplay extends Component {
    /**
     * Creates an instance of this class.
     *
     * @param {Player} player
     *        The `Player` that this class should be attached to.
     *
     * @param {Object} [options]
     *        The key/value store of player options.
     */
    constructor(player: Player, options?: any);
    /**
     * Create the `Component`'s DOM element
     *
     * @return {Element}
     *         The element that was created.
     */
    createEl(): Element;
    contentEl_: Element;
    dispose(): void;
    textNode_: any;
    /**
     * Updates the displayed time according to the `updateContent` function which is defined in the child class.
     *
     * @param {Event} [event]
     *          The `timeupdate`, `ended` or `seeking` (if enableSmoothSeeking is true) event that caused this function to be called.
     */
    update(event?: Event): void;
    /**
     * Updates the time display text node with a new time
     *
     * @param {number} [time=0] the time to update to
     *
     * @private
     */
    private updateTextNode_;
    formattedTime_: any;
    /**
     * To be filled out in the child class, should update the displayed time
     * in accordance with the fact that the current time has changed.
     *
     * @param {Event} [event]
     *        The `timeupdate`  event that caused this to run.
     *
     * @listens Player#timeupdate
     */
    updateContent(event?: Event): void;
    /**
     * The text that is added to the `TimeDisplay` for screen reader users.
     *
     * @type {string}
     * @private
     */
    private labelText_;
    /**
     * The text that should display over the `TimeDisplay`s controls. Added to for localization.
     *
     * @type {string}
     * @protected
     *
     * @deprecated in v7; controlText_ is not used in non-active display Components
     */
    protected controlText_: string;
}
import Component from '../../component.js';
import type Player from '../../player';
//# sourceMappingURL=time-display.d.ts.map