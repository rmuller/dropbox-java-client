package eu.infomas.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Copied from: <a href="http://stackoverflow.com/questions/3089151">Specifying an order
 * to junit 4 tests at the Method level (not class level)</a>
 * 
 * @author Original Author is <a href="http://stackoverflow.com/users/378167/michael-d">michael-d</a>
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @since infomas-asl 3.0.2
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {

    public int value();
}
