package com.daacs.framework.serializer;

import ma.glasnost.orika.DefaultFieldMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.ClassMapBuilder;
import ma.glasnost.orika.metadata.Type;
import ma.glasnost.orika.property.PropertyResolverStrategy;

/**
 * Created by chostetter on 8/9/16.
 */
public class GroovyMapperFactory extends DefaultMapperFactory {

    GroovyMapperFactory(MapperFactoryBuilder<?, ?> builder) {
        super(builder);
        addClassMapBuilderFactory(new GroovyClassMapBuilderFactory());
    }

    public class GroovyClassMapBuilderFactory extends ClassMapBuilder.Factory {

        public GroovyClassMapBuilderFactory() {
            defaults = new DefaultFieldMapper[0];
        }

        @Override
        protected <A, B> boolean appliesTo(Type<A> aType, Type<B> bType) {
            return true;
        }

        @Override
        protected <A, B> ClassMapBuilder<A, B> newClassMapBuilder(
                Type<A> aType, Type<B> bType,
                MapperFactory mapperFactory,
                PropertyResolverStrategy propertyResolver,
                DefaultFieldMapper[] defaults) {

            if(defaults == null){
                defaults = new DefaultFieldMapper[0];
            }

            final ClassMapBuilder<A, B> builder = super.newClassMapBuilder(aType, bType, mapperFactory, propertyResolver, defaults);
            return builder.exclude("metaClass");
        }

    }
}