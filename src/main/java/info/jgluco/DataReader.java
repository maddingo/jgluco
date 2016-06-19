package info.jgluco;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.usb.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
                try {
                    List<UsbEndpoint> usbEndpoints = iface.getUsbEndpoints();
                    log.info("Endpoints: " + usbEndpoints.size());
                    int count = 0;
                    UsbEndpoint inEp = null;
                    UsbEndpoint outEp = null;
                    for (UsbEndpoint ep : usbEndpoints) {
                        switch(ep.getDirection()) {
                            case UsbConst.ENDPOINT_DIRECTION_IN:
                                inEp = ep;
                                break;
                            case UsbConst.ENDPOINT_DIRECTION_OUT:
                                outEp = ep;
                                break;
                        }
                        log.info(String.format("%d: %s", ++count, endpointAsString(ep)));
                    }
                    List<?> result = readData(inEp, outEp);

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

    private static List<?> readData(UsbEndpoint inEp, UsbEndpoint outEp) {
        Objects.requireNonNull(inEp);
        Objects.requireNonNull(outEp);

        List<String> result = new LinkedList<>();

        UsbPipe inPipe = inEp.getUsbPipe();
        try {
            inPipe.syncSubmit(result);
        } catch (UsbException e) {
            new RuntimeException(e);
        }
        return result;
    }

    private static String endpointAsString(UsbEndpoint ep) {

        StringBuilder sb = new StringBuilder();
        sb.append("addr: ").append(String.format("%02x", ep.getUsbEndpointDescriptor().bEndpointAddress())).append(", ");
        byte epType = ep.getType();
        switch(epType) {
            case UsbConst.ENDPOINT_TYPE_CONTROL:
                sb.append("Type CONTROL, ");
                break;
            case UsbConst.ENDPOINT_TYPE_BULK:
                sb.append("Type BULK, ");
                break;
            case UsbConst.ENDPOINT_TYPE_INTERRUPT:
                sb.append("Type INTERRUPT, ");
                break;
            case UsbConst.ENDPOINT_TYPE_ISOCHRONOUS:
                sb.append("Type ISOCHRONOUS, ");
                break;
            default:
                sb.append("Unknown type: ").append(epType).append(", ");
                break;
        }
        sb.append(", ");
        byte direction = ep.getDirection();
        switch(direction) {
            case UsbConst.ENDPOINT_DIRECTION_IN:
                sb.append("direction IN, ");
                break;
            case UsbConst.ENDPOINT_DIRECTION_OUT:
                sb.append("direction OUT, ");
                break;
            default:
                sb.append("Unknown direction: ").append(direction).append(", ");
                break;
        }

        return sb.toString();
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
