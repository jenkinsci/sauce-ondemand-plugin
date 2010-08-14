#!/bin/bash
export SELENIUM_DRIVER='sauce-ondemand:?os=Linux&browser=firefox&browser-version=3.'
exec mvn test
