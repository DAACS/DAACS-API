package com.daacs.framework.serializer;

import com.daacs.model.ListItemMappable;
import com.daacs.model.dto.ListItemDTOMappable;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Created by alandistasio on 10/24/16.
 */
public class ListObjectMapper extends CustomMapper<List<ListItemDTOMappable>, List<ListItemMappable>> {

    MapperFactory mapperFactory;

    ListObjectMapper(MapperFactory customMapperFactory) {
        mapperFactory = customMapperFactory;
    }

    @Override
    public void mapAtoB(List<ListItemDTOMappable> aItems,
                        List<ListItemMappable> bItems,
                        MappingContext context) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        Type<ListItemDTOMappable> sourceType = context.getResolvedSourceType().getNestedType(0);
        Type<ListItemMappable> destinationType = context.getResolvedDestinationType().getNestedType(0);

        aItems.stream().forEach(aItem -> {
            boolean added = false;
            for (ListItemMappable bItem: bItems) {
                if (StringUtils.isNotBlank(aItem.getId())
                    && StringUtils.isNotBlank(bItem.getId())
                    && StringUtils.equals(aItem.getId(), bItem.getId())) {
                    mapper.map(aItem, bItem);
                    added = true;
                    break;
                }
            }
            if (!added) {
                bItems.add(mapper.map(aItem, sourceType, destinationType, context));
            }
        });
    }
}
