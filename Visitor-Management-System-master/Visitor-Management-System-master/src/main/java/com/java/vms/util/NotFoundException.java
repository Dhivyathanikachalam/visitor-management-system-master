package com.java.vms.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotFoundException extends RuntimeException {

    final Logger LOGGER = LoggerFactory.getLogger(NotFoundException.class);

    public NotFoundException() {
        super();
    }

    public NotFoundException(final String message) {
        super(message);
        LOGGER.error(message);
    }

}
