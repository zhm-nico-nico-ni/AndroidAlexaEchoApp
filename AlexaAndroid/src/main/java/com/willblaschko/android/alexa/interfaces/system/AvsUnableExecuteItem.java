package com.willblaschko.android.alexa.interfaces.system;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * Created by ggec on 2017/4/13.
 */

public class AvsUnableExecuteItem extends AvsItem {
    public final String unparsedDirective;
    /*
    * Error Type	Description
UNEXPECTED_INFORMATION_RECEIVED	  The directive sent to your client was malformed or the payload does not conform to the directive specification.
UNSUPPORTED_OPERATION	The operation specified by the namespace/name in the directive's header are not supported by the client.
INTERNAL_ERROR	An error occurred while the device was handling the directive and the error does not fall into the specified categories
     */
    public final String type;

    public final String message;

    public AvsUnableExecuteItem(String unparsedDirective, String type, String message) {
        super(null, null);
        this.unparsedDirective = unparsedDirective;
        this.type = type;
        this.message = message;
    }
}
