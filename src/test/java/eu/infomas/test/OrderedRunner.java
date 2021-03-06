package eu.infomas.test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * Copied from: <a href="http://stackoverflow.com/questions/3089151">Specifying an order
 * to junit 4 tests at the Method level (not class level)</a>
 * 
 * @author Original Author is <a href="http://stackoverflow.com/users/378167/michael-d">michael-d</a>
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @since infomas-asl 3.0.2
 */
public final class OrderedRunner extends BlockJUnit4ClassRunner {

    public OrderedRunner(final Class<?> clz) throws InitializationError {
        super(clz);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        final List<FrameworkMethod> list = super.computeTestMethods();
        Collections.sort(list, new Comparator<FrameworkMethod>() {
            @Override
            public int compare(final FrameworkMethod f1, final FrameworkMethod f2) {
                final Order o1 = f1.getAnnotation(Order.class);
                final Order o2 = f2.getAnnotation(Order.class);
                if (o1 == null || o2 == null) {
                    return -1;
                }
                return o1.value() - o2.value();
            }
        });
        return list;
    }
}
