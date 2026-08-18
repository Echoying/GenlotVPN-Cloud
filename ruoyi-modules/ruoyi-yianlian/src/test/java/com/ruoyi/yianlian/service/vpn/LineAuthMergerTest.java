package com.ruoyi.yianlian.service.vpn;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LineAuthMergerTest
{
    @Test
    public void splitIds_skipsBlank()
    {
        List<String> ids = LineAuthMerger.splitIds("1, 2,,3");
        assertEquals(3, ids.size());
        assertEquals("1", ids.get(0));
    }

    @Test
    public void mergeSource_dedupsAndKeepsOrder()
    {
        Map<String, LinkedHashSet<String>> acc = new LinkedHashMap<String, LinkedHashSet<String>>();
        String key = LineAuthMerger.itemKey("10", "20");
        LineAuthMerger.mergeSource(acc, key, "部门");
        LineAuthMerger.mergeSource(acc, key, "用户");
        LineAuthMerger.mergeSource(acc, key, "部门");
        List<String> sources = LineAuthMerger.sourcesOf(acc, key);
        assertEquals("部门", sources.get(0));
        assertEquals("用户", sources.get(1));
        assertEquals(2, sources.size());
    }
}
