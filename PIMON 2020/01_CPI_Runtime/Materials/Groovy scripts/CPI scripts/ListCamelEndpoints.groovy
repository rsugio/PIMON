import com.sap.gateway.ip.core.customdev.util.Message
import org.apache.camel.CamelContext
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import org.osgi.framework.ServiceReference

Message processData(Message message) {

    StringBuilder builder = new StringBuilder()

    BundleContext bundleContext = FrameworkUtil.getBundle(Message).bundleContext
    ServiceReference[] camelContextServiceReferences = bundleContext.getAllServiceReferences(CamelContext.name, null)
    camelContextServiceReferences.each { serviceReference ->
        CamelContext camelContext = bundleContext.getService(serviceReference) as CamelContext
        builder << "\nOSGi bundle: ${serviceReference.bundle.symbolicName}\n"
        builder << "Camel context: ${camelContext.name}\n"
        builder << "Endpoints:\n"
        camelContext.endpoints.each { endpoint ->
            builder << "\t${endpoint.endpointUri}\n"
        }
        bundleContext.ungetService(serviceReference)
    }

    message.body = builder.toString()

    return message

}