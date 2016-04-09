package info.jgluco;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.usb.*;
import java.util.List;

@Slf4j
public class DataReader {
    public enum UsbId {
        BAYER_CONTOUR_USB(0x1a79, 0x7410);

        @Getter
        private final short productId;

        @Getter
        private final short vendorId;

        UsbId(int vendorId, int productId) {
            this.vendorId = (short) vendorId;
            this.productId = (short) productId;
        }
    }

    public static void main(String[] args) {
        try {
            UsbServices services = UsbHostManager.getUsbServices();
            UsbHub hub = services.getRootUsbHub();
            UsbDevice device = findDevice(hub, UsbId.BAYER_CONTOUR_USB.getVendorId(), UsbId.BAYER_CONTOUR_USB.getProductId());
            if (device != null) {
                log.info("Found Device");
                UsbConfiguration config = device.getActiveUsbConfiguration();
                UsbInterface iface = config.getUsbInterface((byte) 0);
                iface.claim((usbInterface) -> { return true;});
                UsbControlIrp irp = device.createUsbControlIrp((byte)(UsbConst.REQUESTTYPE_TYPE_CLASS | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE), (byte) 0x01, (short) 2, (short) 1);
                try {
                    List usbEndpoints = iface.getUsbEndpoints();
                    log.info("Endpoints: " + usbEndpoints.size());
                } finally {
                    iface.release();
                }
            } else {
                log.info("No Device found");
            }
        } catch (UsbException e) {
            log.error(null, e);
        }

    }

    public static UsbDevice findDevice(UsbHub hub, short vendorId, short productId) {
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
                return device;
            }
            if (device.isUsbHub()) {
                device = findDevice((UsbHub) device, vendorId, productId);
                if (device != null) {
                    return device;
                }
            }
        }
        return null;
    }
}
