package com.tempest.menus;

import com.tempest.menus.entity.Menus;
import com.tempest.menus.repository.MenusRepository;
import com.tempest.menus.service.MenusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenusApplicationTests {

    @Mock
    private MenusRepository menusRepository;

    @InjectMocks
    private MenusService menusService;

    @Test
    void shouldSearchByKeywordAcrossNameMaterialAndTag() {
        Menus menu = new Menus();
        menu.setId(1);
        menu.setName("番茄炒蛋");
        menu.setMaterial("番茄");
        menu.setTag("快手菜");

        when(menusRepository.findActiveByKeyword("番茄")).thenReturn(List.of(menu));

        List<Menus> result = menusService.findByKeyword("番茄");

        assertThat(result).containsExactly(menu);
    }

    @Test
    void shouldReturnAllMenusWhenKeywordIsBlank() {
        Menus menu = new Menus();
        menu.setId(2);
        menu.setName("西红柿豆腐");

        when(menusRepository.findByDeleteFlag(0)).thenReturn(List.of(menu));

        List<Menus> result = menusService.findByKeyword("   ");

        assertThat(result).containsExactly(menu);
    }
}
