export default Menu;
/** @import Player from '../player' */
/**
 * The Menu component is used to build popup menus, including subtitle and
 * captions selection menus.
 *
 * @extends Component
 */
declare class Menu extends Component {
    /**
     * Create an instance of this class.
     *
     * @param {Player} player
     *        the player that this component should attach to
     *
     * @param {Object} [options]
     *        Object of option names and values
     *
     */
    constructor(player: Player, options?: any);
    menuButton_: any;
    focusedChild_: number;
    boundHandleBlur_: (e: any) => void;
    boundHandleTapClick_: (e: any) => void;
    /**
     * Add event listeners to the {@link MenuItem}.
     *
     * @param {Object} component
     *        The instance of the `MenuItem` to add listeners to.
     *
     */
    addEventListenerForItem(component: any): void;
    /**
     * Remove event listeners from the {@link MenuItem}.
     *
     * @param {Object} component
     *        The instance of the `MenuItem` to remove listeners.
     *
     */
    removeEventListenerForItem(component: any): void;
    /**
     * This method will be called indirectly when the component has been added
     * before the component adds to the new menu instance by `addItem`.
     * In this case, the original menu instance will remove the component
     * by calling `removeChild`.
     *
     * @param {Object} component
     *        The instance of the `MenuItem`
     */
    removeChild(component: any): void;
    /**
     * Add a {@link MenuItem} to the menu.
     *
     * @param {Object|string} component
     *        The name or instance of the `MenuItem` to add.
     *
     */
    addItem(component: any | string): void;
    /**
     * Create the `Menu`s DOM element.
     *
     * @return {Element}
     *         the element that was created
     */
    createEl(): Element;
    contentEl_: Element;
    dispose(): void;
    /**
     * Called when a `MenuItem` loses focus.
     *
     * @param {Event} event
     *        The `blur` event that caused this function to be called.
     *
     * @listens blur
     */
    handleBlur(event: Event): void;
    /**
     * Called when a `MenuItem` gets clicked or tapped.
     *
     * @param {Event} event
     *        The `click` or `tap` event that caused this function to be called.
     *
     * @listens click,tap
     */
    handleTapClick(event: Event): void;
    /**
     * Move to next (lower) menu item for keyboard users.
     */
    stepForward(): void;
    /**
     * Move to previous (higher) menu item for keyboard users.
     */
    stepBack(): void;
    /**
     * Set focus on a {@link MenuItem} in the `Menu`.
     *
     * @param {Object|string} [item=0]
     *        Index of child item set focus on.
     */
    focus(item?: any | string): void;
}
import Component from '../component.js';
import type Player from '../player';
//# sourceMappingURL=menu.d.ts.map