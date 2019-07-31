/* 
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
import {
  Title,
  Button,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListCheck,
  DataListAction,
  DataListToggle,
  DataListContent,
  DataListItemCells
} from '@patternfly/react-core';
import {ContentPage} from '../ContentPage';
 
export interface DeviceActivityPageProps {
}
 
export class DeviceActivityPage extends React.Component<DeviceActivityPageProps> {
    
  public constructor(props: DeviceActivityPageProps) {
    super(props);
    this.state = {
      expanded: [],
      isOpen1: false,
      isOpen2: false,
      isOpen3: false
    };

    this.onToggle1 = isOpen1 => {
      this.setState({ isOpen1 });
    };

    this.onToggle2 = isOpen2 => {
      this.setState({ isOpen2 });
    };

  }

  public render(): React.ReactNode {
    const toggle = id => {
      const expanded = this.state.expanded;
      const index = expanded.indexOf(id);
      const newExpanded =
        index >= 0 ? [...expanded.slice(0, index), ...expanded.slice(index + 1, expanded.length)] : [...expanded, id];
      this.setState(() => ({ expanded: newExpanded }));
    };
    return (
        <DataList aria-label="device-activity">
          <DataListItem aria-labelledby="device-activity-item">
            <DataListItemRow>
              <DataListItemCells
                dataListCells={[
                  <DataListCell key="device-activity-cell">
                    <Title headingLevel="h3" size="2xl">
                      Device Activity
                    </Title>
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          <DataListItem aria-labelledby="simple-item2">
            <DataListItemRow>
              <DataListToggle
                onClick={() => toggle('ex-toggle1')}
                isExpanded={this.state.expanded.includes('ex-toggle1')}
                id="ex-toggle1"
                aria-controls="ex-expand1"
              </>
              <DataListItemCells
                dataListCells={[
                  <DataListCell key="secondary content2" width="1">
                    <div>
                      <b id="width-ex3-item1">Firefox</b>
                      <p>IP: 111.111.111.111</p>
                      <p>Last Access: NaN</p>
                    </div>
                  </DataListCell>,
                  <DataListCell isFilled={false} alignRight key="secondary content4">
                    <Button>Logout</Button>
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
            <DataListContent
              aria-label="Primary Content Details"
              id="ex-expand1"
              isHidden={!this.state.expanded.includes('ex-toggle1')}
            >
              <div>
                <p>Client:</p>
                <p>Started:</p>
                <p>Expires:</p>
              </div>

            </DataListContent>
          </DataListItem>

          <DataListItem aria-labelledby="simple-item1">
            <DataListItemRow>
              <DataListToggle
                onClick={() => toggle('ex-toggle2')}
                isExpanded={this.state.expanded.includes('ex-toggle2')}
                id="ex-toggle2"
                aria-controls="ex-expand2"
              </>
            <DataListItemCells
              dataListCells={[
                <DataListCell key="secondary content2" width="1">
                  <div>
                    <b id="width-ex3-item1">Opera 22.33</b>
                    <p>IP: 111.111.111.111</p>
                    <p>Last Access: NaN</p>
                  </div>
                </DataListCell>,
                <DataListCell isFilled={false} alignRight key="secondary content4">
                  <Button>Logout</Button>
                </DataListCell>
              ]}
            />
          </DataListItemRow>
          <DataListContent
            aria-label="Primary Content Details"
            id="ex-expand2"
            isHidden={!this.state.expanded.includes('ex-toggle2')}
          >
            <div>
              <p>Client:</p>
              <p>Started:</p>
              <p>Expires:</p>
            </div>

          </DataListContent>
        </DataListItem>
          <DataListItem aria-labelledby="simple-item1">
            <DataListItemRow>
              <DataListItemCells
                dataListCells={[
                  <DataListCell key="primary content3">
                    <a href="http://">View all history</a>
                  </DataListCell>
                ]}
              />
              <DataListAction
                aria-labelledby="check-action-item2 check-action-action2"
                id="check-action-action2"
                aria-label="Actions"
              >
                <Button>Logout All Devices</Button>
              </DataListAction>
            </DataListItemRow>
          </DataListItem>
        </DataList>
    );
  }
};
