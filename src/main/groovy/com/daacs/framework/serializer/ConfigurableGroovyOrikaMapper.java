package com.daacs.framework.serializer;

import ma.glasnost.orika.*;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by chostetter on 7/8/16.
 */

public abstract class ConfigurableGroovyOrikaMapper implements MapperFacade {

    private MapperFacade facade;
    private GroovyMapperFactory factory;

    public ConfigurableGroovyOrikaMapper() {
        factory = new GroovyMapperFactory(new DefaultMapperFactory.Builder());
        configure(factory);
        facade = factory.getMapperFacade();
    }

    protected abstract void configure(MapperFactory mapperFactory);

    public <S, D> D map(S sourceObject, Class<D> destinationClass) {
        return facade.map(sourceObject, destinationClass);
    }

    public <S, D> D map(S sourceObject, Class<D> destinationClass, MappingContext context) {
        return facade.map(sourceObject, destinationClass, context);
    }

    public <S, D> void map(S sourceObject, D destinationObject) {
        facade.map(sourceObject, destinationObject);
    }

    public <S, D> void map(S sourceObject, D destinationObject, MappingContext context) {
        facade.map(sourceObject, destinationObject, context);
    }

    public <S, D> void map(S sourceObject, D destinationObject, Type<S> sourceType, Type<D> destinationType) {
        facade.map(sourceObject, destinationObject, sourceType, destinationType);
    }

    public <S, D> void map(S sourceObject, D destinationObject, Type<S> sourceType, Type<D> destinationType, MappingContext context) {
        facade.map(sourceObject, destinationObject, sourceType, destinationType, context);
    }

    public <S, D> Set<D> mapAsSet(Iterable<S> source, Class<D> destinationClass) {
        return facade.mapAsSet(source, destinationClass);
    }

    public <S, D> Set<D> mapAsSet(Iterable<S> source, Class<D> destinationClass, MappingContext context) {
        return facade.mapAsSet(source, destinationClass, context);
    }

    public <S, D> Set<D> mapAsSet(S[] source, Class<D> destinationClass) {
        return facade.mapAsSet(source, destinationClass);
    }

    public <S, D> Set<D> mapAsSet(S[] source, Class<D> destinationClass, MappingContext context) {
        return facade.mapAsSet(source, destinationClass, context);
    }

    public <S, D> List<D> mapAsList(Iterable<S> source, Class<D> destinationClass) {
        return facade.mapAsList(source, destinationClass);
    }

    public <S, D> List<D> mapAsList(Iterable<S> source, Class<D> destinationClass, MappingContext context) {
        return facade.mapAsList(source, destinationClass, context);
    }

    public <S, D> List<D> mapAsList(S[] source, Class<D> destinationClass) {
        return facade.mapAsList(source, destinationClass);
    }

    public <S, D> List<D> mapAsList(S[] source, Class<D> destinationClass, MappingContext context) {
        return facade.mapAsList(source, destinationClass, context);
    }

    public <S, D> D[] mapAsArray(D[] destination, Iterable<S> source, Class<D> destinationClass) {
        return facade.mapAsArray(destination, source, destinationClass);
    }

    public <S, D> D[] mapAsArray(D[] destination, S[] source, Class<D> destinationClass) {
        return facade.mapAsArray(destination, source, destinationClass);
    }

    public <S, D> D[] mapAsArray(D[] destination, Iterable<S> source, Class<D> destinationClass, MappingContext context) {
        return facade.mapAsArray(destination, source, destinationClass, context);
    }

    public <S, D> D[] mapAsArray(D[] destination, S[] source, Class<D> destinationClass, MappingContext context) {
        return facade.mapAsArray(destination, source, destinationClass, context);
    }

    public <S, D> D map(S sourceObject, Type<S> sourceType, Type<D> destinationType) {
        return facade.map(sourceObject, sourceType, destinationType);
    }

    public <S, D> D map(S sourceObject, Type<S> sourceType, Type<D> destinationType, MappingContext context) {
        return facade.map(sourceObject, sourceType, destinationType, context);
    }

    public <S, D> Set<D> mapAsSet(Iterable<S> source, Type<S> sourceType, Type<D> destinationType) {
        return facade.mapAsSet(source, sourceType, destinationType);
    }

    public <S, D> Set<D> mapAsSet(Iterable<S> source, Type<S> sourceType, Type<D> destinationType, MappingContext context) {
        return facade.mapAsSet(source, sourceType, destinationType, context);
    }

    public <S, D> Set<D> mapAsSet(S[] source, Type<S> sourceType, Type<D> destinationType) {
        return facade.mapAsSet(source, sourceType, destinationType);
    }

    public <S, D> Set<D> mapAsSet(S[] source, Type<S> sourceType, Type<D> destinationType, MappingContext context) {
        return facade.mapAsSet(source, sourceType, destinationType, context);
    }

    public <S, D> List<D> mapAsList(Iterable<S> source, Type<S> sourceType, Type<D> destinationType) {
        return facade.mapAsList(source, sourceType, destinationType);
    }

    public <S, D> List<D> mapAsList(Iterable<S> source, Type<S> sourceType, Type<D> destinationType, MappingContext context) {
        return facade.mapAsList(source, sourceType, destinationType, context);
    }

    public <S, D> List<D> mapAsList(S[] source, Type<S> sourceType, Type<D> destinationType) {
        return facade.mapAsList(source, sourceType, destinationType);
    }

    public <S, D> List<D> mapAsList(S[] source, Type<S> sourceType, Type<D> destinationType, MappingContext context) {
        return facade.mapAsList(source, sourceType, destinationType, context);
    }

    public <S, D> D[] mapAsArray(D[] destination, Iterable<S> source, Type<S> sourceType, Type<D> destinationType) {
        return facade.mapAsArray(destination, source, sourceType, destinationType);
    }

    public <S, D> D[] mapAsArray(D[] destination, S[] source, Type<S> sourceType, Type<D> destinationType) {
        return facade.mapAsArray(destination, source, sourceType, destinationType);
    }

    public <S, D> D[] mapAsArray(D[] destination, Iterable<S> source, Type<S> sourceType, Type<D> destinationType, MappingContext context) {
        return facade.mapAsArray(destination, source, sourceType, destinationType, context);
    }

    public <S, D> D[] mapAsArray(D[] destination, S[] source, Type<S> sourceType, Type<D> destinationType, MappingContext context) {
        return facade.mapAsArray(destination, source, sourceType, destinationType, context);
    }

    public <S, D> void mapAsCollection(Iterable<S> source, Collection<D> destination, Class<D> destinationClass) {
        facade.mapAsCollection(source, destination, destinationClass);
    }

    public <S, D> void mapAsCollection(Iterable<S> source, Collection<D> destination, Class<D> destinationClass, MappingContext context) {
        facade.mapAsCollection(source, destination, destinationClass, context);
    }

    public <S, D> void mapAsCollection(S[] source, Collection<D> destination, Class<D> destinationCollection) {
        facade.mapAsCollection(source, destination, destinationCollection);
    }

    public <S, D> void mapAsCollection(S[] source, Collection<D> destination, Class<D> destinationCollection, MappingContext context) {
        facade.mapAsCollection(source, destination, destinationCollection, context);
    }

    public <S, D> void mapAsCollection(Iterable<S> source, Collection<D> destination, Type<S> sourceType, Type<D> destinationType) {
        facade.mapAsCollection(source, destination, sourceType, destinationType);
    }

    public <S, D> void mapAsCollection(S[] source, Collection<D> destination, Type<S> sourceType, Type<D> destinationType) {
        facade.mapAsCollection(source, destination, sourceType, destinationType);
    }

    public <S, D> void mapAsCollection(Iterable<S> source, Collection<D> destination, Type<S> sourceType, Type<D> destinationType,
                                       MappingContext context) {
        facade.mapAsCollection(source, destination, sourceType, destinationType, context);
    }

    public <S, D> void mapAsCollection(S[] source, Collection<D> destination, Type<S> sourceType, Type<D> destinationType,
                                       MappingContext context) {
        facade.mapAsCollection(source, destination, sourceType, destinationType, context);
    }

    public <S, D> D convert(S source, Type<S> sourceType, Type<D> destinationType, String converterId) {
        return facade.convert(source, sourceType, destinationType, converterId);
    }


    public <S, D> D convert(S source, Class<D> destinationClass, String converterId) {
        return facade.convert(source, destinationClass, converterId);
    }


    public <S, D> D newObject(S source, Type<? extends D> destinationClass, MappingContext context) {
        return facade.newObject(source, destinationClass, context);
    }

    public <Sk, Sv, Dk, Dv> Map<Dk, Dv> mapAsMap(Map<Sk, Sv> source, Type<? extends Map<Sk, Sv>> sourceType,
                                                 Type<? extends Map<Dk, Dv>> destinationType) {
        return facade.mapAsMap(source, sourceType, destinationType);
    }

    public <Sk, Sv, Dk, Dv> Map<Dk, Dv> mapAsMap(Map<Sk, Sv> source, Type<? extends Map<Sk, Sv>> sourceType,
                                                 Type<? extends Map<Dk, Dv>> destinationType, MappingContext context) {
        return facade.mapAsMap(source, sourceType, destinationType, context);
    }

    public <S, Dk, Dv> Map<Dk, Dv> mapAsMap(Iterable<S> source, Type<S> sourceType, Type<? extends Map<Dk, Dv>> destinationType) {
        return facade.mapAsMap(source, sourceType, destinationType);
    }

    public <S, Dk, Dv> Map<Dk, Dv> mapAsMap(Iterable<S> source, Type<S> sourceType, Type<? extends Map<Dk, Dv>> destinationType,
                                            MappingContext context) {
        return facade.mapAsMap(source, sourceType, destinationType, context);
    }

    public <S, Dk, Dv> Map<Dk, Dv> mapAsMap(S[] source, Type<S> sourceType, Type<? extends Map<Dk, Dv>> destinationType) {
        return facade.mapAsMap(source, sourceType, destinationType);
    }

    public <S, Dk, Dv> Map<Dk, Dv> mapAsMap(S[] source, Type<S> sourceType, Type<? extends Map<Dk, Dv>> destinationType,
                                            MappingContext context) {
        return facade.mapAsMap(source, sourceType, destinationType, context);
    }

    public <Sk, Sv, D> List<D> mapAsList(Map<Sk, Sv> source, Type<? extends Map<Sk, Sv>> sourceType, Type<D> destinationType) {
        return facade.mapAsList(source, sourceType, destinationType);
    }

    public <Sk, Sv, D> List<D> mapAsList(Map<Sk, Sv> source, Type<? extends Map<Sk, Sv>> sourceType, Type<D> destinationType,
                                         MappingContext context) {
        return facade.mapAsList(source, sourceType, destinationType, context);
    }

    public <Sk, Sv, D> Set<D> mapAsSet(Map<Sk, Sv> source, Type<? extends Map<Sk, Sv>> sourceType, Type<D> destinationType) {
        return facade.mapAsSet(source, sourceType, destinationType);
    }

    public <Sk, Sv, D> Set<D> mapAsSet(Map<Sk, Sv> source, Type<? extends Map<Sk, Sv>> sourceType, Type<D> destinationType,
                                       MappingContext context) {
        return facade.mapAsSet(source, sourceType, destinationType, context);
    }

    public <Sk, Sv, D> D[] mapAsArray(D[] destination, Map<Sk, Sv> source, Type<? extends Map<Sk, Sv>> sourceType, Type<D> destinationType) {
        return facade.mapAsArray(destination, source, sourceType, destinationType);
    }

    public <Sk, Sv, D> D[] mapAsArray(D[] destination, Map<Sk, Sv> source, Type<? extends Map<Sk, Sv>> sourceType, Type<D> destinationType,
                                      MappingContext context) {
        return facade.mapAsArray(destination, source, sourceType, destinationType, context);
    }

    public <S, D> MappingStrategy resolveMappingStrategy(S sourceObject, java.lang.reflect.Type rawAType, java.lang.reflect.Type rawBType,
                                                         boolean mapInPlace, MappingContext context) {
        return facade.resolveMappingStrategy(sourceObject, rawAType, rawBType, mapInPlace, context);
    }

    public <S, D> BoundMapperFacade<S, D> dedicatedMapperFor(Type<S> sourceType, Type<D> destinationType) {
        return factory.getMapperFacade(sourceType, destinationType);
    }

    public <S, D> BoundMapperFacade<S, D> dedicatedMapperFor(Type<S> sourceType, Type<D> destinationType, boolean containsCycles) {
        return factory.getMapperFacade(sourceType, destinationType, containsCycles);
    }

    public <A, B> BoundMapperFacade<A, B> dedicatedMapperFor(Class<A> aType, Class<B> bType) {
        return factory.getMapperFacade(aType, bType);
    }

    public <A, B> BoundMapperFacade<A, B> dedicatedMapperFor(Class<A> aType, Class<B> bType, boolean containsCycles) {
        return factory.getMapperFacade(aType, bType, containsCycles);
    }

    public void factoryModified(MapperFactory factory) {
        facade.factoryModified(factory);
    }
}


