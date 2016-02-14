
package cz.habarta.typescript.generator.ext;

import com.fasterxml.jackson.annotation.*;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.*;


public class TypeGuardsForJackson2PolymorphismExtension extends EmitterExtension {

    @Override
    public boolean generatesRuntimeCode() {
        return true;
    }

    @Override
    public void emitObjects(Writer writer, Settings settings, TsModel model) {
        for (TsBeanModel tsBean : model.getBeans()) {
            if (tsBean.getJavaModel() != null) {
                final Class<?> beanClass = tsBean.getJavaModel().getBeanClass();
                final JsonSubTypes jsonSubTypes = beanClass.getAnnotation(JsonSubTypes.class);
                final JsonTypeInfo jsonTypeInfo = beanClass.getAnnotation(JsonTypeInfo.class);
                if (jsonSubTypes != null && jsonTypeInfo != null && jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY) {
                    final String propertyName = jsonTypeInfo.property();
                    for (JsonSubTypes.Type subType : jsonSubTypes.value()) {
                        String propertyValue = null;
                        if (jsonTypeInfo.use() == JsonTypeInfo.Id.NAME) {
                            if (subType.name().equals("")) {
                                final JsonTypeName jsonTypeName = subType.value().getAnnotation(JsonTypeName.class);
                                if (jsonTypeName != null) {
                                    propertyValue = jsonTypeName.value();
                                }
                            } else {
                                propertyValue = subType.name();
                            }
                        }
                        if (propertyValue != null) {
                            final String baseTypeName = tsBean.getName().toString();
                            final String subTypeName = findTypeName(subType.value(), model);
                            if (baseTypeName != null && subTypeName != null) {
                                writer.writeIndentedLine("");
                                emitTypeGuard(writer, settings, baseTypeName, subTypeName, propertyName, propertyValue);
                            }
                        }
                    }
                }
            }
        }
    }

    static String findTypeName(Class<?> beanClass, TsModel model) {
        for (TsBeanModel bean : model.getBeans()) {
            if (bean.getJavaModel().getBeanClass().equals(beanClass)) {
                return bean.getName().toString();
            }
        }
        return null;
    }

// Example:
//    function isCartesianPoint(point: Point): point is CartesianPoint {
//        return point.type === "cartesian";
//    }
    static void emitTypeGuard(Writer writer, Settings settings, String baseType, String subType, String propertyName, String propertyValue) {
        final String argument = Character.toLowerCase(baseType.charAt(0)) + baseType.substring(1);
        writer.writeIndentedLine(String.format("function is%s(%s: %s): %s is %s {", subType, argument, baseType, argument, subType));
        writer.writeIndentedLine(String.format("%sreturn %s.%s === %s;", settings.indentString, argument, propertyName, settings.quotes + propertyValue + settings.quotes));
        writer.writeIndentedLine("}");
    }

}