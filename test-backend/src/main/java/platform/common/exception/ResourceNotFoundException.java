package platform.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String id) {
        super(id);
    }

    public ResourceNotFoundException(String type, String id) {
        super(type + ":" + id);
    }
}
