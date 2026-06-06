package org.apache.dubbo.demo;

/**
 * 示例服务实现 — Provider 端的业务逻辑。
 */
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return "Hello " + name + ", this is mini-dubbo!";
    }
}
