package net.onelitefeather.cygnus.setup.item;

/**
 * Holds all item tag identifiers used during the setup process.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.6.0
 */
public final class SetupItemId {

    public static final byte MAP_SELECTION  = (byte) 0x00;
    public static final byte SAVE_DATA      = (byte) 0x01;
    public static final byte DATA           = (byte) 0x02;
    public static final byte PAGE           = (byte) 0x03;
    public static final byte PAGES          = (byte) 0x04;
    public static final byte LEAVE_PAGE     = (byte) 0x05;
    public static final byte SURVIVOR       = (byte) 0x06;
    public static final byte SPAWNS         = (byte) 0x07;
    public static final byte LEAVE_MODE     = (byte) 0x08;
    public static final byte CREEK_ROUTES   = (byte) 0x09;
    public static final byte CREEK_NEW      = (byte) 0x0A;
    public static final byte CREEK_UNDO     = (byte) 0x0B;
    public static final byte CREEK_LIST     = (byte) 0x0C;
    public static final byte CREEK_LEAVE    = (byte) 0x0D;

    private SetupItemId() {
        // Nothing to do here
    }
}
